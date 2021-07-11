package i5.las2peer.services.noracleService.api;

import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.services.noracleService.model.QuestionRelation;
import i5.las2peer.services.noracleService.model.QuestionRelationList;

public interface INoracleQuestionRelationService {

	QuestionRelation createQuestionRelation(String spaceId, String name, String questionId1, String questionId2,
			Boolean directed) throws ServiceInvocationException;

	QuestionRelation getQuestionRelation(String relationId) throws ServiceInvocationException;

	QuestionRelationList getQuestionRelations(String spaceId, String order, Integer limit, Integer startAt)
			throws ServiceInvocationException;

	QuestionRelation changeQuestionRelation(String relationId, String name, String questionId1,
			String questionId2, Boolean directed) throws ServiceInvocationException;

}
