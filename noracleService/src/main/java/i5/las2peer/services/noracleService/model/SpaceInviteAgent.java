package i5.las2peer.services.noracleService.model;

import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;

public class SpaceInviteAgent extends UserAgentImpl {

	public SpaceInviteAgent(String spaceSecret) throws AgentOperationFailedException, CryptoException {
		super(CryptoTools.generateKeyPair(), spaceSecret, CryptoTools.generateSalt());
	}

	@Override
	public void receiveMessage(Message message, AgentContext c) throws MessageException {
		// do nothing
	}

}
