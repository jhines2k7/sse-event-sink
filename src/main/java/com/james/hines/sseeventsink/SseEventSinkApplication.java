package com.james.hines.sseeventsink;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class SseEventSinkApplication {
	interface SseSink {
		String INPUT_CHANNEL = "sseevents";

		@Input
		SubscribableChannel sseevents();
	}

	@Component
	class SseService {
        private final List<ResponseBodyEmitter> emitters = new ArrayList<>();

        SseEmitter emitter;

	    public SseService() {
            this.emitter = new SseEmitter();
        }

        void publish(String event) {
            emitters.forEach(emitter -> {
                try {
                    emitter.send(event, MediaType.APPLICATION_JSON);
                } catch (IOException e) {
                    emitter.complete();
                    emitters.remove(emitter);
                }
            });
        }

        SseEmitter getMessages() {
            emitters.add(emitter);
            emitter.onCompletion(() -> emitters.remove(emitter));
            return emitter;
        }
    }

	@EnableBinding(SseSink.class)
	class SseEventsSink {
	    @Autowired
        SseService sseService;

        @StreamListener(SseSink.INPUT_CHANNEL)
		void consumeEvent(String event) {
			LoggerFactory.getLogger(SseEventSinkApplication.class).info("Consuming event: '{}'", event);
			sseService.publish(event);
		}
	}

	@Controller
    class SseEventsController {
	    @Autowired
	    SseService sseService;

        @GetMapping(path = "/events/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter subscribe() {
            return sseService.getMessages();
        }

        @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
        public Resource index() {
            return new ClassPathResource("static/index.html");
        }
    }

	public static void main(String[] args) {
		SpringApplication.run(SseEventSinkApplication.class, args);
	}
}
