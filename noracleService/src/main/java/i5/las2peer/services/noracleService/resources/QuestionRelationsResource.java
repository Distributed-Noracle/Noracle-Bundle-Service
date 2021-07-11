package i5.las2peer.services.noracleService.resources;

import com.google.gson.Gson;
import i5.las2peer.api.Context;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.restMapper.ExceptionEntity;
import i5.las2peer.services.noracleService.NoracleQuestionRelationService;
import i5.las2peer.services.noracleService.NoracleQuestionService;
import i5.las2peer.services.noracleService.NoracleService;
import i5.las2peer.services.noracleService.NoracleVoteService;
import i5.las2peer.services.noracleService.api.INoracleQuestionRelationService;
import i5.las2peer.services.noracleService.model.*;
import i5.las2peer.services.noracleService.pojo.ChangeQuestionRelationPojo;
import i5.las2peer.services.noracleService.pojo.CreateRelationPojo;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

public class QuestionRelationsResource implements INoracleQuestionRelationService {

	public static final String RESOURCE_NAME = "relations";

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_HTML)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_CREATED,
			message = "Relation successfully created"),
			@ApiResponse(
					code = HttpURLConnection.HTTP_UNAUTHORIZED,
					message = "You have to be logged in to create a relation",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public Response createQuestionRelation(@PathParam("spaceId") String spaceId, @ApiParam(
			required = true) CreateRelationPojo createRelationPojo) throws ServiceInvocationException {
		QuestionRelation rel = createQuestionRelation(spaceId, createRelationPojo.getName(),
				createRelationPojo.getFirstQuestionId(), createRelationPojo.getSecondQuestionId(),
				createRelationPojo.isDirected());
		try {
			Gson gson = new Gson();
			String createRelationPojoJson = gson.toJson(createRelationPojo);

			JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
			JSONObject obj = (JSONObject) p.parse(createRelationPojoJson);
			obj.put("spaceId", spaceId);
			obj.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3, obj.toJSONString());

			// Try to log training data
			try {
				String from = getQuestionText(createRelationPojo.getFirstQuestionId());
				String to = getQuestionText(createRelationPojo.getSecondQuestionId());
				JSONObject trainingData = new JSONObject();
				trainingData.put("unit", spaceId);
				trainingData.put("from", from);
				trainingData.put("to", to);
				if (createRelationPojo.isDirected())
					Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_40, trainingData.toString());
				else
					Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_41, trainingData.toString());
			} catch (ServiceInvocationException | NullPointerException e) {
				Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_ERROR_40, e.getMessage());
			}
			Question q1 = null;
			Question q2 = null;
			Serializable rmiResult = Context.get().invoke(
					new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(), NoracleService.API_VERSION),
					"getQuestion", createRelationPojo.getFirstQuestionId());
			if (rmiResult instanceof Question) {
				q1 = (Question) rmiResult;
			}

			Serializable rmiResult2 = Context.get().invoke(
					new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(), NoracleService.API_VERSION),
					"getQuestion", createRelationPojo.getSecondQuestionId());
			if (rmiResult instanceof Question) {
				q2 = (Question) rmiResult2;
			}

			if (q1 != null && q2 != null) {
				try {
					Instant timestamp = Instant.parse(q1.getTimestampCreated());
					Instant timestamp2 = Instant.parse(q2.getTimestampCreated());
					if (timestamp.isBefore(timestamp2) && q2.getDepth() < 1) {
						changeQuestionDepth(q2.getQuestionId(), q1.getDepth() + 1);
						JSONObject obj2 = new JSONObject();
						obj2.put("spaceId", spaceId);
						obj2.put("qid", q2.getQuestionId());
						obj2.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
						obj2.put("depth", q1.getDepth() + 1);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_10, obj2.toJSONString());

					} else {
						if (q1.getDepth() < 1) {
							changeQuestionDepth(q1.getQuestionId(), q2.getDepth() + 1);
							JSONObject obj2 = new JSONObject();
							obj2.put("spaceId", spaceId);
							obj2.put("qid", q1.getQuestionId());
							obj2.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
							obj2.put("depth", q2.getDepth() + 1);
							Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_10, obj2.toJSONString());
						}
					}
				} catch (Exception e) {

				}
			}

			return Response.created(new URI(null, null,
					SpacesResource.RESOURCE_NAME + "/" + spaceId + "/" + RESOURCE_NAME + "/" + rel.getRelationId(),
					null)).build();
		} catch (URISyntaxException | ParseException e) {
			throw new InternalServerErrorException(e);
		}
	}

	public Question changeQuestionDepth(String questionId, int depth) throws ServiceInvocationException {
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(), NoracleService.API_VERSION),
				"changeQuestionDepth", questionId, depth);
		if (rmiResult instanceof Question) {
			return (Question) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	private String getQuestionText(String questionId) throws ServiceInvocationException {
		String qText = "";
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(), NoracleService.API_VERSION),
				"getQuestion", questionId);
		if (rmiResult instanceof Question)
			qText = ((Question) rmiResult).getText();
		return qText;
	}

	@Override
	public QuestionRelation createQuestionRelation(String spaceId, String name, String questionId1, String questionId2,
			Boolean directed) throws ServiceInvocationException {
		Serializable rmiResult = Context.get()
				.invoke(new ServiceNameVersion(NoracleQuestionRelationService.class.getCanonicalName(),
						NoracleService.API_VERSION), "createQuestionRelation", spaceId, name, questionId1, questionId2,
						directed);
		if (rmiResult instanceof QuestionRelation) {
			return (QuestionRelation) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	@Override
	@GET
	@Path("/{relationId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_OK,
			message = "A question relation object from the network",
			response = QuestionRelation.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_BAD_REQUEST,
					message = "No relation id given",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_FORBIDDEN,
					message = "Access Denied",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_NOT_FOUND,
					message = "Relation Not Found",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public QuestionRelation getQuestionRelation(@PathParam("relationId") String relationId)
			throws ServiceInvocationException {
		Serializable rmiResult = Context.get()
				.invoke(new ServiceNameVersion(NoracleQuestionRelationService.class.getCanonicalName(),
						NoracleService.API_VERSION), "getQuestionRelation", relationId);
		if (rmiResult instanceof QuestionRelation) {
			return (QuestionRelation) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_OK,
			message = "A list of relations from the network",
			response = QuestionRelationList.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_BAD_REQUEST,
					message = "No space id given",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_FORBIDDEN,
					message = "Access Denied",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public Response getQuestions(@PathParam("spaceId") String spaceId, @QueryParam("order") String order,
			@QueryParam("limit") Integer limit, @QueryParam("startAt") Integer startAt)
			throws ServiceInvocationException {
		QuestionRelationList questionRelationList = getQuestionRelations(spaceId, order, limit, startAt);
		
		VotedQuestionRelationList votedQuestionRelationList = new VotedQuestionRelationList();
		for (QuestionRelation questionRelation:questionRelationList) {
			VotedQuestionRelation votedQuestionRelation = new VotedQuestionRelation(questionRelation);
			String objectId = RelationVotesResource.buildObjectId(spaceId, questionRelation.getRelationId());
			Serializable rmiResult = Context.get().invoke(
					new ServiceNameVersion(NoracleVoteService.class.getCanonicalName(), NoracleService.API_VERSION),
					"getAllVotes", objectId);
			if (rmiResult instanceof VoteList) {
				votedQuestionRelation.setVotes((VoteList) rmiResult);
			}
			votedQuestionRelationList.add(votedQuestionRelation);
		}
		
		ResponseBuilder responseBuilder = Response.ok(votedQuestionRelationList);

		String queryOrder = order != null ? "order=" + order : "";
		String queryLimit = limit != null ? "limit=" + Integer.toString(limit) : "";
		String queryStartAt = "";
		if (startAt != null && limit != null) {
			if (order.equalsIgnoreCase("desc")) {
				queryStartAt = "startat=" + Integer.toString(startAt - limit);
			} else {
				queryStartAt = "startat=" + Integer.toString(startAt + limit);
			}
		}
		String nextLinkStr = "";
		for (String param : new String[] { queryOrder, queryLimit, queryStartAt }) {
			if (param != null && !param.isEmpty()) {
				if (nextLinkStr.isEmpty()) {
					nextLinkStr += "?";
				} else {
					nextLinkStr += "&";
				}
				nextLinkStr += param;
			}
		}
		if (!nextLinkStr.isEmpty()) {
			responseBuilder.header(HttpHeaders.LINK, "<" + nextLinkStr + ">; rel=\"next\"");
		}
		return responseBuilder.build();
	}

	@Override
	public QuestionRelationList getQuestionRelations(String spaceId, String order, Integer limit, Integer startAt)
			throws ServiceInvocationException {
		Serializable rmiResult = Context.get()
				.invoke(new ServiceNameVersion(NoracleQuestionRelationService.class.getCanonicalName(),
						NoracleService.API_VERSION), "getQuestionRelations", spaceId, order, limit, startAt);
		if (rmiResult instanceof QuestionRelationList) {
			return (QuestionRelationList) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	@PUT
	@Path("/{relationId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_OK,
			message = "Changes a question relation",
			response = QuestionRelation.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_BAD_REQUEST,
					message = "No relation id given",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_FORBIDDEN,
					message = "Access Denied",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_NOT_FOUND,
					message = "Relation Not Found",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public QuestionRelation changeQuestionRelation(@PathParam("relationId") String relationId, @ApiParam(
			required = true) ChangeQuestionRelationPojo changeQuestionRelationPojo) throws ServiceInvocationException {

		Gson gson = new Gson();
		String changeRelationPojoJson = gson.toJson(changeQuestionRelationPojo);

		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONObject obj = (JSONObject) p.parse(changeRelationPojoJson);
			obj.put("relId", relationId);
			obj.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_4, obj.toJSONString());
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Try to log training data
		try {
			String from = getQuestionText(changeQuestionRelationPojo.getQuestionId1());
			String to = getQuestionText(changeQuestionRelationPojo.getQuestionId2());
			JSONObject trainingData = new JSONObject();
			trainingData.put("from", from);
			trainingData.put("to", to);
			if (changeQuestionRelationPojo.getDirected())
				Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_42, trainingData.toString());
			else
				Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_43, trainingData.toString());
		} catch (ServiceInvocationException e) {
			/* getDirected will return null at this point */	
//			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_ERROR_42,
//					changeQuestionRelationPojo.getDirected().toString());
		}
		return changeQuestionRelation(relationId, changeQuestionRelationPojo.getName(),
				changeQuestionRelationPojo.getQuestionId1(), changeQuestionRelationPojo.getQuestionId2(),
				changeQuestionRelationPojo.getDirected());
	}

	@Override
	public QuestionRelation changeQuestionRelation(String relationId, String name, String questionId1,
			String questionId2, Boolean directed) throws ServiceInvocationException {
		Serializable rmiResult = Context.get()
				.invoke(new ServiceNameVersion(NoracleQuestionRelationService.class.getCanonicalName(),
						NoracleService.API_VERSION), "changeQuestionRelation", relationId, name, questionId1,
						questionId2, directed);
		if (rmiResult instanceof QuestionRelation) {
			return (QuestionRelation) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	@Path("/{relationId}/" + QuestionVotesResource.RESOURCE_NAME)
	public RelationVotesResource votes() {
		return new RelationVotesResource();
	}

}
