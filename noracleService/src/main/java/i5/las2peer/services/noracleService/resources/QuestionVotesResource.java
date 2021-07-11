package i5.las2peer.services.noracleService.resources;

import com.google.gson.Gson;
import i5.las2peer.api.Context;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ResourceNotFoundException;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.restMapper.ExceptionEntity;
import i5.las2peer.services.noracleService.NoracleQuestionService;
import i5.las2peer.services.noracleService.NoracleService;
import i5.las2peer.services.noracleService.NoracleVoteService;
import i5.las2peer.services.noracleService.api.INoracleVoteService;
import i5.las2peer.services.noracleService.model.Question;
import i5.las2peer.services.noracleService.model.Vote;
import i5.las2peer.services.noracleService.model.VoteList;
import i5.las2peer.services.noracleService.pojo.SetVotePojo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.net.HttpURLConnection;

@Api
public class QuestionVotesResource implements INoracleVoteService {

	public static final String RESOURCE_NAME = "votes";

	@PUT
	@Path("/{agentId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_OK,
			message = "Vote successfully set",
			response = Vote.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_UNAUTHORIZED,
					message = "You have to be logged in to vote",
					response = ExceptionEntity.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public Vote putSetQuestionVote(@PathParam("spaceId") String spaceId, @PathParam("questionId") String questionId,
			@PathParam("agentId") String agentId, @ApiParam(
					required = true) SetVotePojo setVotePojo)
			throws ServiceInvocationException {
		String objectId = buildObjectId(spaceId, questionId);

		Gson gson = new Gson();
		String voteJSON = gson.toJson(setVotePojo);
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONObject obj = (JSONObject) p.parse(voteJSON);
			JSONObject attributes = new JSONObject();
			obj.put("spaceId", spaceId);
			obj.put("qId", questionId);

			obj.put("functionName", "putSetQuestionVote");
			obj.put("serviceAlias", "distributed-noracle");
			obj.put("uid", Context.getCurrent().getMainAgent().getIdentifier());
			attributes.put("spaceId", spaceId);
			attributes.put("qId", questionId);
			attributes.put("body", p.parse(voteJSON));
			attributes.put("result", "");
			obj.put("attributes", attributes);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_7, obj.toJSONString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Try to log training data
		try {
			String from = getQuestionText(questionId);
			Integer to = setVotePojo.getValue();
			JSONObject trainingData = new JSONObject();
			trainingData.put("unit", spaceId);
			trainingData.put("from", from);
			trainingData.put("to", to);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_47, trainingData.toString());
		} catch (ServiceInvocationException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_ERROR_47, questionId);
		}
		return setVote(agentId, objectId, setVotePojo.getValue());
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
	public Vote setVote(String agentId, String objectId, int value) throws ServiceInvocationException {
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleVoteService.class.getCanonicalName(), NoracleService.API_VERSION),
				"setVote", agentId, objectId, value);
		if (rmiResult instanceof Vote) {
			return (Vote) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
	}

	@GET
	@Path("/{agentId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_OK,
			message = "Vote successfully retrieved",
			response = Vote.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public Vote getAgentVote(@PathParam("spaceId") String spaceId, @PathParam("questionId") String questionId,
			@PathParam("agentId") String agentId) throws ServiceInvocationException {
		String objectId = buildObjectId(spaceId, questionId);
		return getAgentVote(objectId, agentId);
	}

	@Override
	public Vote getAgentVote(String objectId, String agentId) throws ServiceInvocationException {
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleVoteService.class.getCanonicalName(), NoracleService.API_VERSION),
				"getAgentVote", objectId, agentId);
		Vote vote;
		if (rmiResult instanceof Vote) {
			vote = (Vote) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
		return vote;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses({ @ApiResponse(
			code = HttpURLConnection.HTTP_OK,
			message = "Votes successfully retrieved",
			response = VoteList.class),
			@ApiResponse(
					code = HttpURLConnection.HTTP_INTERNAL_ERROR,
					message = "Internal Server Error",
					response = ExceptionEntity.class) })
	public VoteList getAllVotes(@PathParam("spaceId") String spaceId, @PathParam("questionId") String questionId)
			throws ServiceInvocationException {
		String objectId = buildObjectId(spaceId, questionId);
		return getAllVotes(objectId);
	}

	@Override
	public VoteList getAllVotes(String objectId) throws ServiceInvocationException {
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleVoteService.class.getCanonicalName(), NoracleService.API_VERSION),
				"getAllVotes", objectId);
		VoteList vote;
		if (rmiResult instanceof VoteList) {
			vote = (VoteList) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
		return vote;
	}

	public static String buildObjectId(String spaceId, String questionId) throws ResourceNotFoundException {
		if (questionId != null && !questionId.isEmpty()) {
			return "question-" + questionId;
		} else {
			throw new ResourceNotFoundException("No vote target identifier given");
		}
	}

}
