/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.streams.globalktable.join;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Serialized;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.kafka.streams.annotations.KafkaStreamsProcessor;
import org.springframework.messaging.handler.annotation.SendTo;

/**
 * This is the PR that added this sample:
 * https://github.com/spring-cloud/spring-cloud-stream-samples/pull/112
 */
@SpringBootApplication
public class KafkaStreamsGlobalKTableJoin {

	public static void main(String[] args) {
		SpringApplication.run(KafkaStreamsGlobalKTableJoin.class, args);
	}

	@EnableBinding(KStreamProcessorX.class)
	public static class KStreamToTableJoinApplication {


		@StreamListener
		@SendTo("output")
		public KStream<String, Long> process(@Input("input") KStream<String, Long> userClicksStream,
											 @Input("inputTable") GlobalKTable<String, String> userRegionsTable) {

			return userClicksStream
					.leftJoin(userRegionsTable,
							(name,value) -> name,
							(clicks, region) -> new RegionWithClicks(region == null ? "UNKNOWN" : region, clicks)
							)
					.map((user, regionWithClicks) -> new KeyValue<>(regionWithClicks.getRegion(), regionWithClicks.getClicks()))
					.groupByKey(Serialized.with(Serdes.String(), Serdes.Long()))
					.reduce((firstClicks, secondClicks) -> firstClicks + secondClicks)
					.toStream();
		}
	}


	interface KStreamProcessorX extends KafkaStreamsProcessor {

		@Input("inputTable")
		GlobalKTable<?, ?> inputKTable();
	}

	private static final class RegionWithClicks {

		private final String region;
		private final long clicks;

		public RegionWithClicks(String region, long clicks) {
			if (region == null || region.isEmpty()) {
				throw new IllegalArgumentException("region must be set");
			}
			if (clicks < 0) {
				throw new IllegalArgumentException("clicks must not be negative");
			}
			this.region = region;
			this.clicks = clicks;
		}

		public String getRegion() {
			return region;
		}

		public long getClicks() {
			return clicks;
		}

	}
}
