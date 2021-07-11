package i5.las2peer.services.noracleService;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.execution.*;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.security.*;
import i5.las2peer.services.noracleService.api.INoracleQuestionService;
import i5.las2peer.services.noracleService.model.Question;
import i5.las2peer.services.noracleService.model.QuestionList;
import i5.las2peer.services.noracleService.model.Space;

import java.io.Serializable;
import java.time.Instant;
import java.util.Random;

/**
 * Noracle Question Service
 * 
 * This service is used to handle questions in a distributed Noracle system.
 * 
 */
public class NoracleQuestionService extends Service implements INoracleQuestionService {

	private static final int MAX_QUESTIONS_PER_SPACE = 1000;

	private final Random myRandom;

	public NoracleQuestionService() {
		myRandom = new Random();
	}

	@Override
	public Question createQuestion(String questionSpaceId, String text) throws ServiceInvocationException {
		Agent mainAgent = Context.get().getMainAgent();
		if (questionSpaceId == null || questionSpaceId.isEmpty()) {
			throw new InvocationBadArgumentException("No question space id given");
		} else if (text == null || text.isEmpty()) {
			throw new InvocationBadArgumentException("No question text given");
		} else if (mainAgent instanceof AnonymousAgent) {
			throw new ServiceNotAuthorizedException("You have to be logged in to create a question");
		}
		Space targetSpace;
		Serializable rmiResult = Context.get().invoke(
				new ServiceNameVersion(NoracleSpaceService.class.getCanonicalName(), NoracleService.API_VERSION),
				"getSpace", questionSpaceId);
		if (rmiResult instanceof Space) {
			targetSpace = (Space) rmiResult;
		} else {
			throw new InternalServiceException(
					"Unexpected result (" + rmiResult.getClass().getCanonicalName() + ") of RMI call");
		}
		String targetReaderGroupId = targetSpace.getSpaceReaderGroupId();
		GroupAgent targetReaderGroup;
		try {
			targetReaderGroup = (GroupAgent) Context.get().requestAgent(targetReaderGroupId, mainAgent);
		} catch (AgentNotFoundException | AgentOperationFailedException e) {
			throw new InternalServiceException("Could not fetch reader group agent for space", e);
		} catch (ClassCastException e) {
			throw new InternalServiceException("Agent for space reader group is not a GroupAgent", e);
		} catch (AgentAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Agent not in space reader group", e);
		}
		String questionId = buildQuestionId();
		Envelope env;
		try {
			env = Context.get().createEnvelope(getQuestionEnvelopeIdentifier(questionId), mainAgent);
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not create envelope for question", e);
		}
		env.addReader(targetReaderGroup);
		Question question = new Question(questionId, text, questionSpaceId, mainAgent.getIdentifier(),
				Instant.now().toString());
		env.setContent(question);
		try {
			Context.get().storeEnvelope(env, mainAgent);
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not store question envelope", e);
		}
		if (questionSpaceId != null && !questionSpaceId.isEmpty()) {
			linkQuestionToSpace(questionSpaceId, questionId);
		}
		return question;
	}

	public boolean linkQuestionToSpace(String spaceId, String questionId) {
		int questionNumber;
		for (questionNumber = 1; questionNumber < MAX_QUESTIONS_PER_SPACE; questionNumber++) {
			try {
				Context.get().requestEnvelope(buildSpaceQuestionNumberId(spaceId, questionNumber));
			} catch (EnvelopeNotFoundException e) { // found free question number
				break;
			} catch (Exception e) {
				// XXX logging
			}
		}
		try {
			Envelope spaceQuestionEnv = Context.get()
					.createEnvelope(buildSpaceQuestionNumberId(spaceId, questionNumber));
			spaceQuestionEnv.setPublic();
			spaceQuestionEnv.setContent(questionId);
			Context.get().storeEnvelope(spaceQuestionEnv);
			return true;
		} catch (EnvelopeOperationFailedException | EnvelopeAccessDeniedException e) {
			// TODO exception handling
			e.printStackTrace();
		}
		return false;
	}

	private String buildSpaceQuestionNumberId(String spaceId, int questionNumber) {
		return "spacequestion-" + spaceId + "-" + questionNumber;
	}

	@Override
	public Question getQuestion(String questionId) throws ServiceInvocationException {
		if (questionId == null || questionId.isEmpty()) {
			throw new InvocationBadArgumentException("No question id given");
		}
		Envelope env;
		try {
			env = Context.get().requestEnvelope(getQuestionEnvelopeIdentifier(questionId));
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not fetch question envelope", e);
		} catch (EnvelopeNotFoundException e) {
			throw new ResourceNotFoundException("Question Not Found");
		}
		Question question = (Question) env.getContent();
		return question;
	}

	private String buildQuestionId() {
		String result = "";
		for (int c = 0; c < 10; c++) {
			result += myRandom.nextInt(10);
		}
		return result;
	}

	private String getQuestionEnvelopeIdentifier(String questionId) {
		return "question-" + questionId;
	}

	@Override
	public QuestionList getQuestions(String spaceId, String order, Integer limit, Integer startAt)
			throws ServiceInvocationException {
		if (spaceId == null || spaceId.isEmpty()) {
			throw new InvocationBadArgumentException("No space id given");
		} else if (limit != null && limit <= 0) {
			throw new InvocationBadArgumentException("Invalid limit given");
		} else if (startAt != null && startAt < 1) {
			throw new InvocationBadArgumentException("Invalid startAt given");
		}
		if (order == null || order.isEmpty()) {
			order = "asc";
		}
		if (limit == null) {
			limit = 10;
		}
		if (startAt == null) {
			startAt = 1;
		}
		QuestionList result = new QuestionList();
		try {
			if (order.equalsIgnoreCase("desc")) {
				for (int questionNumber = startAt; questionNumber > startAt - limit; questionNumber--) {
					if (!retrieveQuestion(result, spaceId, questionNumber)) {
						limit++;
					}
				}
			} else {
				for (int questionNumber = startAt; questionNumber < startAt + limit; questionNumber++) {
					if (!retrieveQuestion(result, spaceId, questionNumber)) {
						limit++;
					}
				}
			}
		} catch (EnvelopeNotFoundException e) {
			// done
		}
		return result;
	}

	private boolean retrieveQuestion(QuestionList result, String spaceId, int questionNumber)
			throws EnvelopeNotFoundException {
		try {
			Envelope spaceQuestionEnv = Context.get()
					.requestEnvelope(buildSpaceQuestionNumberId(spaceId, questionNumber));
			String questionId = (String) spaceQuestionEnv.getContent();
			Envelope questionEnv = Context.get().requestEnvelope(getQuestionEnvelopeIdentifier(questionId));
			Question question = (Question) questionEnv.getContent();
			// TODO check if author is a member of this space?
//			String authorId = question.getAuthorId();
//			if (authorId == null || authorId.isEmpty()) {
//				return false;
//			}
			result.add(question);
			return true;
		} catch (EnvelopeNotFoundException e) {
			throw e;
		} catch (Exception e) {
			// XXX logging
		}
		return false;
	}

	@Override
	public Question changeQuestionText(String questionId, String text) throws ServiceInvocationException {
		if (questionId == null) {
			throw new InvocationBadArgumentException("No question id given");
		}
		try {
			Envelope questionEnvelope = Context.get().requestEnvelope(getQuestionEnvelopeIdentifier(questionId));
			Question question = (Question) questionEnvelope.getContent();
			question.setText(text);
			question.setTimestampLastModified(Instant.now().toString());
			questionEnvelope.setContent(question);
			Context.get().storeEnvelope(questionEnvelope);
			return question;
		} catch (EnvelopeNotFoundException e) {
			throw new ResourceNotFoundException("Question not found");
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not fetch question envelope", e);
		}
	}

	@Override
	public Question changeQuestionDepth(String questionId, int depth) throws ServiceInvocationException {
		if (questionId == null) {
			throw new InvocationBadArgumentException("No question id given");
		}
		try {
			Envelope questionEnvelope = Context.get().requestEnvelope(getQuestionEnvelopeIdentifier(questionId));
			Question question = (Question) questionEnvelope.getContent();
			question.setDepth(depth);
			question.setTimestampLastModified(Instant.now().toString());
			questionEnvelope.setContent(question);
			Context.get().storeEnvelope(questionEnvelope);
			return question;
		} catch (EnvelopeNotFoundException e) {
			throw new ResourceNotFoundException("Question not found");
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not fetch question envelope", e);
		}
	}

}
