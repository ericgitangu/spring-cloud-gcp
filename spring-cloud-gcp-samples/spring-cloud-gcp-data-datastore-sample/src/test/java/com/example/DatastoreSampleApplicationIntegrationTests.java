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

package com.example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.data.datastore.core.DatastoreTemplate;
import org.apache.commons.io.output.TeeOutputStream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests verifies that the datastore-sample works. In order to run it, use the
 *
 * -Dit.datastore=true -Dspring.cloud.gcp.sql.database-name=[...]
 * -Dspring.cloud.gcp.datastore.namespace=[...]
 *
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 */
//please use "-Dit.datastore=true" to enable the tests
@EnabledIfSystemProperty(named = "it.datastore", matches = "true")
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(classes = {
		DatastoreRepositoryExample.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatastoreSampleApplicationIntegrationTests {

	private static PrintStream systemOut;

	private static ByteArrayOutputStream baos;

	final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private CommandLineRunner commandLineRunner;

	@Autowired
	private SingerRepository singerRepository;

	@Autowired
	DatastoreTemplate datastoreTemplate;

	@BeforeAll
	static void checkToRun() {
		systemOut = System.out;
		baos = new ByteArrayOutputStream();
		TeeOutputStream out = new TeeOutputStream(systemOut, baos);
		System.setOut(new PrintStream(out));
	}

	@AfterEach
	void cleanUp() {
		this.datastoreTemplate.deleteAll(Instrument.class);
		this.datastoreTemplate.deleteAll(Band.class);
		this.datastoreTemplate.deleteAll(Singer.class);
	}

	@Test
	void runTests() throws Exception {
		basicTest();
		testCompoundKeyRestResource();
		testQueryReturnStream();
	}

	void basicTest() throws Exception {
		Singer johnDoe = new Singer(null, "John", "Doe", null);
		Singer janeDoe = new Singer(null, "Jane", "Doe", null);
		Singer richardRoe = new Singer(null, "Richard", "Roe", null);
		Singer frodoBaggins = new Singer(null, "Frodo", "Baggins", null);

		Awaitility.await().atMost(15, TimeUnit.SECONDS)
				.until(() -> getSingers("/singers?sort=lastName,ASC").size() == 3);

		List<Singer> singersAsc = getSingers("/singers?sort=lastName,ASC");
		assertThat(singersAsc)
				.as("Verify ASC order")
				.containsExactly(johnDoe, janeDoe, richardRoe);

		List<Singer> singersDesc = getSingers("/singers?sort=lastName,DESC");
		assertThat(singersDesc)
				.as("Verify DESC order")
				.containsExactly(richardRoe, johnDoe, janeDoe);

		sendRequest("/singers", "{\"singerId\": \"singerFrodo\", \"firstName\":" +
						" \"Frodo\", \"lastName\": \"Baggins\"}",
				HttpMethod.POST);

		Awaitility.await().atMost(15, TimeUnit.SECONDS)
				.until(() -> getSingers("/singers?sort=lastName,ASC").size() == 4);

		List<Singer> singersAfterInsertion = getSingers("/singers?sort=lastName,ASC");
		assertThat(singersAfterInsertion)
				.as("Verify post")
				.containsExactly(frodoBaggins, johnDoe, janeDoe, richardRoe);

		sendRequest("/singers/singer1", null, HttpMethod.DELETE);

		Awaitility.await().atMost(15, TimeUnit.SECONDS)
				.until(() -> getSingers("/singers?sort=lastName,ASC").size() == 3);

		List<Singer> singersAfterDeletion = getSingers("/singers?sort=lastName,ASC");
		assertThat(singersAfterDeletion)
				.as("Verify Delete")
				.containsExactly(frodoBaggins, janeDoe, richardRoe);

		assertThat(baos.toString())
				.as("Verify relationships saved in transaction")
				.contains("Relationship links "
						+ "were saved between a singer, bands, and instruments in a single transaction: "
						+ "Singer{singerId='singer2', firstName='Jane', lastName='Doe', "
						+ "albums=[Album{albumName='a', date=2012-01-20}, Album{albumName='b', "
						+ "date=2018-02-12}], firstBand=General Band, bands=General Band,Big Bland Band, "
						+ "personalInstruments=recorder,cow bell}");

		assertThat(
				this.singerRepository.findById("singer2").get().getPersonalInstruments()
						.stream().map(Instrument::getType).collect(Collectors.toList()))
								.containsExactlyInAnyOrder("recorder", "cow bell");

		assertThat(
				this.singerRepository.findById("singer2").get().getBands().stream()
						.map(Band::getName).collect(Collectors.toList()))
								.containsExactlyInAnyOrder("General Band", "Big Bland Band");

		Singer singer3 = this.singerRepository.findById("singer3").get();

		assertThat(
				singer3.getPersonalInstruments()
						.stream().map(Instrument::getType).collect(Collectors.toList()))
								.containsExactlyInAnyOrder("triangle", "marimba");

		assertThat(
				singer3.getBands().stream()
						.map(Band::getName).collect(Collectors.toList()))
								.containsExactlyInAnyOrder("Crooked Still", "Big Bland Band");

		assertThat(singer3.getLastModifiedTime())
				.isAfter(LocalDateTime.parse("2000-01-01T00:00:00"));

		assertThat(baos.toString()).contains("Query by example\n" +
				"Singer{singerId='singer1', firstName='John', lastName='Doe', " +
				"albums=[], firstBand=null, bands=, personalInstruments=}\n" +
				"Singer{singerId='singer2', firstName='Jane', lastName='Doe', " +
				"albums=[Album{albumName='a', date=2012-01-20}");

		assertThat(baos.toString()).contains("Using Pageable parameter\n" +
				"Singer{singerId='singer1', firstName='John', lastName='Doe', " +
				"albums=[], firstBand=null, bands=, personalInstruments=}\n" +
				"Singer{singerId='singer2', firstName='Jane', lastName='Doe', " +
				"albums=[Album{albumName='a', date=2012-01-20}");

		assertThat(baos.toString()).contains("Find by reference with query\n" + "[Richard]");

		assertThat(baos.toString()).contains("Find by reference\n" + "[Richard]");

		assertThat(baos.toString()).contains("This concludes the sample.");
	}

	void testCompoundKeyRestResource() throws IOException {
		String allInstrumentsResponse = sendRequest("/instruments", null, HttpMethod.GET);
		Map<String, Object> parsedResponse = this.mapper.readValue(allInstrumentsResponse,
				new TypeReference<HashMap<String, Object>>() {
				});
		Map instrument = (Map) ((List) ((Map) parsedResponse.get("_embedded")).get("instruments")).get(0);
		String instrumentUrl = (String) ((Map) ((Map) instrument.get("_links")).get("instrument")).get("href");
		String instrumentType = instrument.get("type").toString();
		String instrumentResponse = sendRequest(instrumentUrl, null, HttpMethod.GET);
		// An instrument JSON object is expected. In this case a recorder.
		assertThat(instrumentResponse).contains("{\n" + "  \"type\" : \"" + instrumentType + "\"");
	}

	private String sendRequest(String url, String json, HttpMethod method) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("Content-Type", "application/json");

		HttpEntity<String> entity = new HttpEntity<>(json, map);
		ResponseEntity<String> response = this.restTemplate.exchange(url, method, entity,
				String.class);
		return response.getBody();
	}

	@SuppressWarnings("unchecked")
	private List<Singer> getSingers(String url) throws java.io.IOException {
		String response = this.restTemplate.getForObject(url, String.class);

		Map<String, Object> parsedResponse = this.mapper.readValue(response,
				new TypeReference<HashMap<String, Object>>() {
				});
		List<Map<String, Object>> singerMaps = (List<Map<String, Object>>) ((Map<String, Object>) parsedResponse
				.get("_embedded")).get("singers");

		return singerMaps.stream().map(som -> new Singer(null,
				(String) som.get("firstName"), (String) som.get("lastName"), null))
				.collect(Collectors.toList());
	}

	void testQueryReturnStream() {
		Stream<Singer> streamResult = singerRepository.findStreamOfSingersByLastName("Doe");
		assertThat(streamResult).isInstanceOf(Stream.class);
		streamResult.map(Singer::getLastName).forEach(x -> assertThat(x).isEqualTo("Doe"));
	}
}
