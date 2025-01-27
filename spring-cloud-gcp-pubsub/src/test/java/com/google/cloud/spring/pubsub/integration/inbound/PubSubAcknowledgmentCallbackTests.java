/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.pubsub.integration.inbound;

import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.acks.AcknowledgmentCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
/**
 * Tests for {@link PubSubAcknowledgmentCallback}.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */

class PubSubAcknowledgmentCallbackTests {

	private AcknowledgeablePubsubMessage mockMessage;

	@BeforeEach
	void setUp() {
		this.mockMessage = mock(AcknowledgeablePubsubMessage.class);
	}

	@Test
	void constructor_nullMessageFailsAssert() {
		assertThatThrownBy(() -> new PubSubAcknowledgmentCallback(null, AckMode.MANUAL))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("message to be acknowledged cannot be null");
	}

	@Test
	void constructor_nullAckModeFailsAssert() {
		assertThatThrownBy(() -> new PubSubAcknowledgmentCallback(this.mockMessage, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("ackMode cannot be null");
	}

	@Test
	void acknowledge_acksOnAccept() {
		PubSubAcknowledgmentCallback callback = new PubSubAcknowledgmentCallback(this.mockMessage, AckMode.AUTO);
		callback.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		verify(this.mockMessage).ack();
		assertThat(callback.isAcknowledged()).isTrue();
	}

	@Test
	void acknowledge_nacksOnReject() {
		PubSubAcknowledgmentCallback callback = new PubSubAcknowledgmentCallback(this.mockMessage, AckMode.AUTO);
		callback.acknowledge(AcknowledgmentCallback.Status.REJECT);
		verify(this.mockMessage).nack();
		assertThat(callback.isAcknowledged()).isTrue();
	}

	@Test
	void acknowledge_nacksOnRequeue() {
		PubSubAcknowledgmentCallback callback = new PubSubAcknowledgmentCallback(this.mockMessage, AckMode.AUTO);
		callback.acknowledge(AcknowledgmentCallback.Status.REQUEUE);
		verify(this.mockMessage).nack();
		assertThat(callback.isAcknowledged()).isTrue();
	}

	@Test
	void isAutoAckTrueForAutoMode() {
		PubSubAcknowledgmentCallback callback = new PubSubAcknowledgmentCallback(this.mockMessage, AckMode.AUTO);
		assertThat(callback.isAutoAck()).isTrue();
	}

	@Test
	void isAutoAckTrueForAutoAckMode() {
		PubSubAcknowledgmentCallback callback = new PubSubAcknowledgmentCallback(this.mockMessage, AckMode.AUTO_ACK);
		assertThat(callback.isAutoAck()).isTrue();
	}

	@Test
	void isAutoAckFalseFroManualMode() {
		PubSubAcknowledgmentCallback callback = new PubSubAcknowledgmentCallback(this.mockMessage, AckMode.MANUAL);
		assertThat(callback.isAutoAck()).isFalse();
	}

	@Test
	void isAcknowledgedFalseForNewCallback() {
		PubSubAcknowledgmentCallback callback = new PubSubAcknowledgmentCallback(this.mockMessage, AckMode.MANUAL);
		assertThat(callback.isAcknowledged()).isFalse();
	}

	@Test
	void isAcknowledgedTrueAfterAccept() {
		PubSubAcknowledgmentCallback callback = new PubSubAcknowledgmentCallback(this.mockMessage, AckMode.MANUAL);
		callback.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		assertThat(callback.isAcknowledged()).isTrue();
	}

	@Test
	void isAcknowledgedTrueAfterReject() {
		PubSubAcknowledgmentCallback callback = new PubSubAcknowledgmentCallback(this.mockMessage, AckMode.MANUAL);
		callback.acknowledge(AcknowledgmentCallback.Status.REJECT);
		assertThat(callback.isAcknowledged()).isTrue();
	}

}
