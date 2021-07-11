package i5.las2peer.services.noracleService.api;

import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.services.noracleService.model.NoracleAgentProfile;
import i5.las2peer.services.noracleService.model.SpaceSubscription;
import i5.las2peer.services.noracleService.model.SpaceSubscriptionList;

public interface INoracleAgentService {

	NoracleAgentProfile updateAgentProfile(String agentName) throws ServiceInvocationException;

	NoracleAgentProfile getAgentProfile(String agentId) throws ServiceInvocationException;

	SpaceSubscription subscribeToSpace(String spaceId, String spaceSecret) throws ServiceInvocationException;

	void unsubscribeFromSpace(String spaceId) throws ServiceInvocationException;

	SpaceSubscriptionList getSpaceSubscriptions(String agentId) throws ServiceInvocationException;

	SpaceSubscription updateSpaceSubscription(String agentId, String spaceId, String[] selectedQuestions)
			throws ServiceInvocationException;

}
