package com.mercateo.rest.hateoas.client.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercateo.common.rest.schemagen.JsonHyperSchema;
import com.mercateo.rest.hateoas.client.ListResponse;
import com.mercateo.rest.hateoas.client.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
public class ResponseBuilder {
	@NonNull
	@Getter
	private Client client;
	@NonNull
	private ObjectMapper objectMapper;

	public <S> Optional<Response<S>> buildResponse(@NonNull String responseString, @NonNull Class<S> responseClass) {
		if (responseString.length() == 0) {
			return Optional.of(new ResponseImpl<>(this, null, null));
		}
		JsonNode rawValue = getRawValue(responseString);
		return buildSingleResponse(rawValue, responseClass);
	}

	public <S> Optional<ListResponse<S>> buildListResponse(@NonNull String responseString,
			@NonNull Class<S> responseClass) {
		JsonNode rawValue = getRawValue(responseString);
		JsonHyperSchema jsonHyperSchema = buildSchema(rawValue);
		JsonNode membersNode = rawValue.get("members");
		if (membersNode != null) {
			List<Response<S>> list = new LinkedList<>();
			for (Iterator<JsonNode> iterator = membersNode.elements(); iterator.hasNext();) {
				JsonNode jsonNode = iterator.next();
				list.add(buildSingleResponse(jsonNode, responseClass).get());
			}
			return Optional.of(new ListResponseImpl<>(this, jsonHyperSchema, list));
		} else {
			throw new ProcessingException("There is no members field in the response");
		}
	}

	private JsonHyperSchema buildSchema(JsonNode rawValue) {
		JsonNode schemaElement = rawValue.get("_schema");
		try {
			return objectMapper.treeToValue(schemaElement, JsonHyperSchema.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

	}

	private JsonNode getRawValue(String responseString) {
		JsonNode rawValue;
		try {
			rawValue = objectMapper.readTree(responseString);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return rawValue;
	}

	private <S> Optional<Response<S>> buildSingleResponse(JsonNode rawValue, Class<S> responseClass) {
		try {

			S value = objectMapper.treeToValue(rawValue, responseClass);
			JsonHyperSchema schema = buildSchema(rawValue);
			Response<S> returningResponse = new ResponseImpl<>(this, schema, value);
			return Optional.of(returningResponse);
		} catch (IOException e) {
			throw new ProcessingException("The response class " + responseClass.getName()
					+ " does not fit the response. Response was :" + rawValue);
		}
	}
}
