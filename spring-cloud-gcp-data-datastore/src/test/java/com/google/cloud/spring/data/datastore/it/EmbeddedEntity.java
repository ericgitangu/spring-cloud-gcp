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

package com.google.cloud.spring.data.datastore.it;

import com.google.cloud.spring.data.datastore.core.mapping.Entity;

/**
 * A test entity for Datastore integration tests.
 *
 * @author Dmitry Solomakha
 */
@Entity
class EmbeddedEntity {

	private String stringField;

	EmbeddedEntity(String stringField) {
		this.stringField = stringField;
	}

	String getStringField() {
		return stringField;
	}

	void setStringField(String stringField) {
		this.stringField = stringField;
	}

	@Override
	public String toString() {
		return "EmbeddedEntity{" +
				"stringField='" + stringField + '\'' +
				'}';
	}
}
