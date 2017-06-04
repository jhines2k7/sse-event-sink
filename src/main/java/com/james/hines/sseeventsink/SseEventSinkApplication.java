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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

        void publish(String data) {
            emitters.forEach(emitter -> {
                try {
                    emitter.send(data, MediaType.APPLICATION_JSON);
                } catch (IOException e) {
                    emitters.remove(emitter);
                    emitter.complete();
                }
            });
        }

        SseEmitter getMessages() {
            emitters.add(emitter);
            emitter.onCompletion(() -> emitters.remove(emitter));

            return emitter;
        }
    }

	//@EnableBinding(SseSink.class)
	/*class SseEventsSink {
	    @Autowired
        SseService sseService;

        @StreamListener(SseSink.INPUT_CHANNEL)
		void consumeEvent(String event) {
			LoggerFactory.getLogger(SseEventSinkApplication.class).info("Consuming event: '{}'", event);
			sseService.publish(event);
		}
	}*/

	@Controller
    @EnableBinding(SseSink.class)
    class SseEventsController {
	    //@Autowired
	    //SseService sseService;
        private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

        @CrossOrigin(origins = "http://localhost:9000")
        @GetMapping(path = "/events/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter subscribe() {
            //return sseService.getMessages();
            SseEmitter emitter = new SseEmitter();
            this.emitters.add(emitter);

            emitter.onCompletion(() -> this.emitters.remove(emitter));
            emitter.onTimeout(() -> this.emitters.remove(emitter));

            return emitter;
        }

        @StreamListener(SseSink.INPUT_CHANNEL)
        void consumeEvent(String event) {
            LoggerFactory.getLogger(SseEventSinkApplication.class).info("Consuming event: '{}'", event);
            List<SseEmitter> deadEmitters = new ArrayList<>();
            this.emitters.forEach(emitter -> {
                try {
                    emitter.send(event);
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
