package com.james.hines.sseeventsink;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.http.MediaType;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@SpringBootApplication
public class SseEventSinkApplication {
	interface SseSink {
		String INPUT_CHANNEL = "ideafoundryoccasionapiresponse2";

		@Input
		SubscribableChannel ideafoundryoccasionapiresponse2();
	}

	@Controller
    @EnableBinding(SseSink.class)
    class SseEventsController {
        private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

        @CrossOrigin
        @GetMapping(path = "/events/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter subscribe() {
            SseEmitter emitter = new SseEmitter(60*60*2*1000L);
            this.emitters.add(emitter);

            emitter.onCompletion(() -> this.emitters.remove(emitter));
            emitter.onTimeout(() -> this.emitters.remove(emitter));

            return emitter;
        }

        @StreamListener(SseSink.INPUT_CHANNEL)
        void consume(String data) {
            LoggerFactory.getLogger(SseEventSinkApplication.class).info("Consuming event: '{}'", data);
            List<SseEmitter> deadEmitters = new ArrayList<>();
            this.emitters.forEach(emitter -> {
                SseEmitter.SseEventBuilder builder = SseEmitter.event()
                        .data(data)
                        .id(UUID.randomUUID().toString())
                        .name("ideafoundry-sse")
                        .reconnectTime(5_000L);
                try {
                    emitter.send(builder);
                }
                catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            });

            this.emitters.remove(deadEmitters);
        }
    }

	public static void main(String[] args) {
		SpringApplication.run(SseEventSinkApplication.class, args);
	}
}
