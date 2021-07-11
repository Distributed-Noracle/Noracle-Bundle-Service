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
import i5.las2peer.services.noracleService.api.INoracleQuestionService;
import i5.las2peer.services.noracleService.model.*;
import i5.las2peer.services.noracleService.pojo.ChangeQuestionPojo;
import i5.las2peer.services.noracleService.pojo.CreateQuestionPojo;
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

public class QuestionsResource implements INoracleQuestionService {

	public static final String RESOURCE_NAME = "questions";

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_HTML)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_CREATED,
			message = "Question successfully created"),
			@ApiResponse(
					code = HttpURLConnection.HTTP_BAD_REQUEST,
					message = "No question text given",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_UNAUTHORIZED,
					message = "You have to be logged in to create a question",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public Response createQuestion(@PathParam("spaceId") String questionSpaceId, @ApiParam(
			required = true) CreateQuestionPojo createQuestionPojo) throws ServiceInvocationException {
		Question question = createQuestion(questionSpaceId, createQuestionPojo.getText());
		try {

			Gson gson = new Gson();
			String createRelationPojoJson = gson.toJson(createQuestionPojo);
			JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
			JSONObject obj = new JSONObject();
			JSONObject attributes = new JSONObject();
			obj.put("functionName", "createQuestion");
			obj.put("serviceAlias", "distributed-noracle");
			obj.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
			obj.put("qid", question.getQuestionId());
			attributes.put("spaceId", questionSpaceId);
			attributes.put("userId", Context.getCurrent().getMainAgent().getIdentifier());
			attributes.put("body", p.parse(createRelationPojoJson));
			attributes.put("result", question.getQuestionId());
			obj.put("attributes", attributes);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_5, obj.toJSONString());

			return Response.created(new URI(null, null, SpacesResource.RESOURCE_NAME + "/" + questionSpaceId + "/"
					+ RESOURCE_NAME + "/" + question.getQuestionId(), null)).build();
		} catch (URISyntaxException | ParseException e) {
			throw new InternalServerErrorException(e);
		}
	}

	@POST
	@Path("/{questionId}/relation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_HTML)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_CREATED,
			message = "Question successfully created"),
			@ApiResponse(
					code = HttpURLConnection.HTTP_BAD_REQUEST,
					message = "No question text given",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_UNAUTHORIZED,
					message = "You have to be logged in to create a question",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public Response createRelatedQuestion(@PathParam("spaceId") String questionSpaceId,
			@PathParam("questionId") String fquestionId, @ApiParam(
					required = true) CreateQuestionPojo createQuestionPojo)
			throws ServiceInvocationException {
		Question question = createQuestion(questionSpaceId, createQuestionPojo.getText());
		try {
			// CREATE QUESTION
			Gson gson = new Gson();
			String createQuestionPojoJson = gson.toJson(createQuestionPojo);
			JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
			JSONObject obj = new JSONObject();
			JSONObject attributes = new JSONObject();
			obj.put("functionName", "createQuestion");
			obj.put("serviceAlias", "distributed-noracle");
			obj.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
			obj.put("qid", question.getQuestionId());
			attributes.put("spaceId", questionSpaceId);
			attributes.put("body", p.parse(createQuestionPojoJson));
			attributes.put("result", question.getQuestionId());
			obj.put("attributes", attributes);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_5, obj.toJSONString());

			// CREATE RELATION
			CreateRelationPojo createRelationPojo = new CreateRelationPojo();
			createRelationPojo.setDirected(true);
			createRelationPojo.setFirstQuestionId(fquestionId);
			createRelationPojo.setSecondQuestionId(question.getQuestionId());
			createRelationPojo.setName("FollowUp");

//			QuestionRelation rel = createQuestionRelation(questionSpaceId, createRelationPojo.getName(),
//					createRelationPojo.getFirstQuestionId(), createRelationPojo.getSecondQuestionId(),
//					createRelationPojo.isDirected());
			try {
				String createRelationPojoJson = gson.toJson(createRelationPojo);

				obj = (JSONObject) p.parse(createRelationPojoJson);
				obj.put("spaceId", questionSpaceId);
				obj.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
				Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3, obj.toJSONString());

				// Try to log training data
				try {
					String from = getQuestionText(createRelationPojo.getFirstQuestionId());
					String to = getQuestionText(createRelationPojo.getSecondQuestionId());
					JSONObject trainingData = new JSONObject();
					trainingData.put("unit", questionSpaceId);
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
				Serializable rmiResult = Context
						.get().invoke(
								new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(),
										NoracleService.API_VERSION),
								"getQuestion", createRelationPojo.getFirstQuestionId());
				if (rmiResult instanceof Question) {
					q1 = (Question) rmiResult;
				}

				Serializable rmiResult2 = Context
						.get().invoke(
								new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(),
										NoracleService.API_VERSION),
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
							obj2.put("spaceId", questionSpaceId);
							obj2.put("qid", q2.getQuestionId());
							obj2.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
							obj2.put("depth", q1.getDepth() + 1);
							Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_10, obj2.toJSONString());

						} else {
							if (q1.getDepth() < 1) {
								changeQuestionDepth(q1.getQuestionId(), q2.getDepth() + 1);
								JSONObject obj2 = new JSONObject();
								obj2.put("spaceId", questionSpaceId);
								obj2.put("qid", q1.getQuestionId());
								obj2.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
								obj2.put("depth", q2.getDepth() + 1);
								Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_10,
										obj2.toJSONString());
							}
						}
					} catch (Exception e) {

					}
				}
			} catch (ParseException e) {
				throw new InternalServerErrorException(e);
			}

			return Response.created(new URI(null, null, SpacesResource.RESOURCE_NAME + "/" + questionSpaceId + "/"
					+ RESOURCE_NAME + "/" + question.getQuestionId(), null)).build();
		} catch (URISyntaxException | ParseException e) {
			throw new InternalServerErrorException(e);
		}
	}

	private QuestionRelation createQuestionRelation(String spaceId, String name, String questionId1, String questionId2,
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
	public Question createQuestion(String questionSpaceId, String text) throws ServiceInvocationException {
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(), NoracleService.API_VERSION),
				"createQuestion", questionSpaceId, text);
		if (rmiResult instanceof Question) {
			return (Question) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	@Override
	@GET
	@Path("/{questionId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_OK,
			message = "A question object from the network",
			response = Question.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_BAD_REQUEST,
					message = "No question id given",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_FORBIDDEN,
					message = "Access Denied",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_NOT_FOUND,
					message = "Question Not Found",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public Question getQuestion(@PathParam("questionId") String questionId) throws ServiceInvocationException {
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(), NoracleService.API_VERSION),
				"getQuestion", questionId);
		if (rmiResult instanceof Question) {
			return (Question) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_OK,
			message = "A list of questions from the network",
			response = QuestionList.class),
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
	public Response getQuestionsWeb(@PathParam("spaceId") String spaceId, @QueryParam("order") String order,
			@QueryParam("limit") Integer limit, @QueryParam("startat") Integer startAt)
			throws ServiceInvocationException {
		QuestionList questionList = getQuestions(spaceId, order, limit, startAt);
		
		VotedQuestionList votedQuestionList = new VotedQuestionList();
		for (Question question:questionList) {
			VotedQuestion votedQuestion = new VotedQuestion(question);
			String objectId = QuestionVotesResource.buildObjectId(spaceId, question.getQuestionId());
			Serializable rmiResult = Context.get().invoke(
					new ServiceNameVersion(NoracleVoteService.class.getCanonicalName(), NoracleService.API_VERSION),
					"getAllVotes", objectId);
			if (rmiResult instanceof VoteList) {
				votedQuestion.setVotes((VoteList) rmiResult);
			}
			votedQuestionList.add(votedQuestion);
		}
		
		ResponseBuilder responseBuilder = Response.ok(votedQuestionList);
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
	public QuestionList getQuestions(String spaceId, String order, Integer limit, Integer startAt)
			throws ServiceInvocationException {
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(), NoracleService.API_VERSION),
				"getQuestions", spaceId, order, limit, startAt);
		if (rmiResult instanceof QuestionList) {
			return (QuestionList) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	@PUT
	@Path("/{questionId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_OK,
			message = "The updated question from the network",
			response = Question.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_BAD_REQUEST,
					message = "No question id given",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_FORBIDDEN,
					message = "Access Denied",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_NOT_FOUND,
					message = "Question Not Found",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public Question changeQuestionText(@PathParam("questionId") String questionId,
			ChangeQuestionPojo changeQuestionPojo) throws ServiceInvocationException {

		Gson gson = new Gson();
		String createRelationPojoJson = gson.toJson(changeQuestionPojo);
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONObject obj = (JSONObject) p.parse(createRelationPojoJson);
			obj.put("qId", questionId);
			obj.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_6, obj.toJSONString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return changeQuestionText(questionId, changeQuestionPojo.getText());
	}

	@Override
	public Question changeQuestionText(String questionId, String text) throws ServiceInvocationException {
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleQuestionService.class.getCanonicalName(), NoracleService.API_VERSION),
				"changeQuestionText", questionId, text);
		if (rmiResult instanceof Question) {
			return (Question) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	@Path("/{questionId}/" + QuestionVotesResource.RESOURCE_NAME)
	public QuestionVotesResource votes() {
		return new QuestionVotesResource();
	}

	@Override
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

}