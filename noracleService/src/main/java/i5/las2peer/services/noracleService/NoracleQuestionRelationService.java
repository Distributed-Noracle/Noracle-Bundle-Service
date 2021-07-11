package i5.las2peer.services.noracleService;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.execution.*;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.services.noracleService.api.INoracleQuestionRelationService;
import i5.las2peer.services.noracleService.model.QuestionRelation;
import i5.las2peer.services.noracleService.model.QuestionRelationList;

import java.time.Instant;
import java.util.Random;

/**
 * Noracle QuestionRelation Relation Service
 * 
 * This service is used to handle question relations in a distributed Noracle system.
 * 
 */
public class NoracleQuestionRelationService extends Service implements INoracleQuestionRelationService {

	private static final int MAX_RELATIONS_PER_SPACE = 1000000;

	private final Random myRandom;

	public NoracleQuestionRelationService() {
		this.myRandom = new Random();
	}

	@Override
	public QuestionRelation createQuestionRelation(String spaceId, String name, String questionId1, String questionId2,
			Boolean directed) throws ServiceInvocationException {
		Agent mainAgent = Context.get().getMainAgent();
		if (mainAgent instanceof AnonymousAgent) {
			throw new ServiceAccessDeniedException("You have to be logged in to create a relation");
		}
		String relationId = buildQuestionRelationId();
		Envelope env;
		try {
			env = Context.get().createEnvelope(getQuestionRelationEnvelopeIdentifier(relationId));
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not create envelope for relation", e);
		}
		env.setPublic();
		String strNow = Instant.now().toString();
		QuestionRelation relation = new QuestionRelation(relationId, spaceId, mainAgent.getIdentifier(), name,
				questionId1, questionId2, directed, strNow);
		env.setContent(relation);
		try {
			Context.get().storeEnvelope(env, mainAgent);
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not store relation envelope", e);
		}
		if (spaceId != null && !spaceId.isEmpty()) {
			linkQuestionRelationToSpace(spaceId, relationId);
		}
		return relation;
	}

	public boolean linkQuestionRelationToSpace(String spaceId, String relationId) {
		int relationNumber;
		for (relationNumber = 1; relationNumber < MAX_RELATIONS_PER_SPACE; relationNumber++) {
			try {
				Context.get().requestEnvelope(buildSpaceQuestionRelationNumberId(spaceId, relationNumber));
			} catch (EnvelopeNotFoundException e) { // found free question number
				break;
			} catch (Exception e) {
				// XXX logging
			}
		}
		try {
			Envelope spaceQuestionRelationEnv = Context.get()
					.createEnvelope(buildSpaceQuestionRelationNumberId(spaceId, relationNumber));
			spaceQuestionRelationEnv.setPublic();
			spaceQuestionRelationEnv.setContent(relationId);
			Context.get().storeEnvelope(spaceQuestionRelationEnv);
			return true;
		} catch (EnvelopeOperationFailedException | EnvelopeAccessDeniedException e) {
			// TODO exception handling
			e.printStackTrace();
		}
		return false;
	}

	private String buildSpaceQuestionRelationNumberId(String spaceId, int relationNumber) {
		return "spacerelation-" + spaceId + "-" + relationNumber;
	}

	@Override
	public QuestionRelation getQuestionRelation(String relationId) throws ServiceInvocationException {
		if (relationId == null || relationId.isEmpty()) {
			throw new InvocationBadArgumentException("No relation id given");
		}
		Envelope env;
		try {
			env = Context.get().requestEnvelope(getQuestionRelationEnvelopeIdentifier(relationId));
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not fetch relation envelope", e);
		} catch (EnvelopeNotFoundException e) {
			throw new ResourceNotFoundException("Relation Not Found");
		}
		QuestionRelation relation = (QuestionRelation) env.getContent();
		return relation;
	}

	private String buildQuestionRelationId() {
		String result = "";
		for (int c = 0; c < 10; c++) {
			result += myRandom.nextInt(10);
		}
		return result;
	}

	private String getQuestionRelationEnvelopeIdentifier(String relationId) {
		return "relation-" + relationId;
	}

	@Override
	public QuestionRelationList getQuestionRelations(String spaceId, String order, Integer limit, Integer startAt)
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
		int direction = order.equalsIgnoreCase("desc") ? -1 : 1;
		QuestionRelationList result = new QuestionRelationList();
		for (int relationNumber = startAt; relationNumber < startAt + direction * limit; relationNumber += direction) {
			try {
				Envelope spaceQuestionRelationEnv;
				try {
					spaceQuestionRelationEnv = Context.get()
							.requestEnvelope(buildSpaceQuestionRelationNumberId(spaceId, relationNumber));
				} catch (EnvelopeNotFoundException e) {
					break;
				}
				String questionId = (String) spaceQuestionRelationEnv.getContent();
				Envelope questionEnv = Context.get().requestEnvelope(getQuestionRelationEnvelopeIdentifier(questionId));
				result.add((QuestionRelation) questionEnv.getContent());
			} catch (Exception e) {
				// XXX logging
			}
		}
		return result;
	}

	@Override
	public QuestionRelation changeQuestionRelation(String relationId, String name, String questionId1,
			String questionId2, Boolean directed) throws ServiceInvocationException {
		if (relationId == null) {
			throw new InvocationBadArgumentException("No relation id given");
		}
		try {
			Envelope relationEnvelope = Context.get()
					.requestEnvelope(getQuestionRelationEnvelopeIdentifier(relationId));
			QuestionRelation relation = (QuestionRelation) relationEnvelope.getContent();
			if (name != null) {
				relation.setName(name);
			}
			if (questionId1 != null) {
				relation.setFirstQuestionId(questionId1);
			}
			if (questionId2 != null) {
				relation.setSecondQuestionId(questionId2);
			}
			if (directed != null) {
				relation.setDirected(directed);
			}
			relation.setTimestampLastModified(Instant.now().toString());
			relationEnvelope.setContent(relation);
			Context.get().storeEnvelope(relationEnvelope);
			return relation;
		} catch (EnvelopeNotFoundException e) {
			throw new ResourceNotFoundException("Relation not found");
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not fetch question envelope", e);
		}
	}

}
