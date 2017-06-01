package com.james.hines.sseeventsink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.SubscribableChannel;

@SpringBootApplication
public class SseEventSinkApplication {
	interface SseSink {
		String INPUT_CHANNEL = "sseevents";

		@Input
		SubscribableChannel sseevents();
	}

	@EnableBinding(SseSink.class)
	public class SseEventsSink {
		@StreamListener(SseSink.INPUT_CHANNEL)
		void consumeEvent(String event) {
			LoggerFactory.getLogger(SseEventSinkApplication.class).info("Consuming event: '{}'", event);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(SseEventSinkApplication.class, args);
	}
}
