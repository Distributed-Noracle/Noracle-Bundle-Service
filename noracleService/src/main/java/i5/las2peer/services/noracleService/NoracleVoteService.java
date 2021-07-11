package i5.las2peer.services.noracleService;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.InvocationBadArgumentException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.services.noracleService.api.INoracleVoteService;
import i5.las2peer.services.noracleService.model.Vote;
import i5.las2peer.services.noracleService.model.VoteEntry;
import i5.las2peer.services.noracleService.model.VoteList;

public class NoracleVoteService extends Service implements INoracleVoteService {

	private static final int MAX_VOTES_PER_OBJECT = 1000000;

	@Override
	public Vote setVote(String agentId, String objectId, int vote) throws ServiceInvocationException {
		Agent mainAgent = Context.get().getMainAgent();
		if (objectId == null || objectId.isEmpty()) {
			throw new InvocationBadArgumentException("No object id given");
		} else if (mainAgent instanceof AnonymousAgent) {
			throw new ServiceAccessDeniedException("You have to be logged in to vote");
		}
		String persEnvId = getAgentVoteEnvelopeIdentifier(agentId, objectId);
		try {
			try {
				Envelope persEnv = Context.get().requestEnvelope(persEnvId);
				VoteEntry voteEntry = (VoteEntry) persEnv.getContent();
				int pubIndex = voteEntry.getPubIndex();
				String pubEnvId = getPublicVoteEnvelopeIdentifier(objectId, pubIndex);
				Vote pubVote = updateOrCreatePubVoteEnv(pubEnvId, vote, mainAgent.getIdentifier());
				voteEntry = (VoteEntry) persEnv.getContent();
				voteEntry.setVote(pubVote);
				persEnv.setContent(voteEntry);
				Context.get().storeEnvelope(persEnv);
				return pubVote;
			} catch (EnvelopeNotFoundException e) {
				Envelope persEnv = Context.get().createEnvelope(persEnvId);
				String pubEnvId = null;
				int pubIndex = -1;
				for (int num = 1; num < MAX_VOTES_PER_OBJECT; num++) {
					try {
						pubEnvId = getPublicVoteEnvelopeIdentifier(objectId, num);
						Context.get().requestEnvelope(pubEnvId);
					} catch (EnvelopeNotFoundException e2) {
						// found non taken vote index
						pubIndex = num;
						break;
					} catch (Exception e2) {
						// XXX logging
					}
				}
				if (pubEnvId == null || pubEnvId.isEmpty()) {
					throw new InternalServiceException("Public envelope id is null");
				} else if (pubIndex == -1) {
					throw new InternalServiceException("Too many votes for this object");
				}
				Vote pubVote = updateOrCreatePubVoteEnv(pubEnvId, vote, mainAgent.getIdentifier());
				VoteEntry voteEntry = new VoteEntry(objectId, pubIndex, pubVote);
				persEnv.setContent(voteEntry);
				Context.get().storeEnvelope(persEnv);
				return pubVote;
			}
		} catch (EnvelopeAccessDeniedException e) {
			throw new ServiceAccessDeniedException("Envelope Access Denied");
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Could not create envelope for vote", e);
		}
	}

	private Vote updateOrCreatePubVoteEnv(String pubEnvId, int vote, String agentId)
			throws EnvelopeAccessDeniedException, EnvelopeOperationFailedException {
		Vote pubVote;
		try {
			Envelope pubEnv = Context.get().requestEnvelope(pubEnvId);
			pubVote = (Vote) pubEnv.getContent();
			pubVote.setValue(vote);
			pubEnv.setContent(pubVote);
			Context.get().storeEnvelope(pubEnv);
		} catch (EnvelopeNotFoundException e) {
			Envelope pubEnv = Context.get().createEnvelope(pubEnvId);
			pubEnv.setPublic();
			pubVote = new Vote(vote, agentId);
			pubEnv.setContent(pubVote);
			Context.get().storeEnvelope(pubEnv);
		}
		return pubVote;
	}

	private static String getAgentVoteEnvelopeIdentifier(String agentId, String objectId) {
		return "agentvote-" + agentId + "-" + objectId;
	}

	private static String getPublicVoteEnvelopeIdentifier(String objectId, int num) {
		return "vote-" + objectId + "-" + num;
	}

	@Override
	public Vote getAgentVote(String objectId, String agentId) throws ServiceInvocationException {
		String persEnvId = getAgentVoteEnvelopeIdentifier(agentId, objectId);
		try {
			Envelope persEnv = Context.get().requestEnvelope(persEnvId);
			VoteEntry voteEntry = (VoteEntry) persEnv.getContent();
			return voteEntry.getVote();
		} catch (EnvelopeAccessDeniedException e) {
			throw new InternalServiceException("Someone hijacked your vote envelope", e);
		} catch (EnvelopeNotFoundException e) {
			return new Vote(0, agentId);
		} catch (EnvelopeOperationFailedException e) {
			throw new InternalServiceException("Retrieving vote failed", e);
		}
	}

	@Override
	public VoteList getAllVotes(String objectId) throws ServiceInvocationException {
		VoteList result = new VoteList();
		for (int num = 1; num < MAX_VOTES_PER_OBJECT; num++) {
			try {
				String pubEnvId = getPublicVoteEnvelopeIdentifier(objectId, num);
				Envelope pubEnv = Context.get().requestEnvelope(pubEnvId);
				Vote vote = (Vote) pubEnv.getContent();
				int normalizedVal = vote.getValue();
				if (normalizedVal > 1) {
					normalizedVal = 1;
				} else if (normalizedVal < -1) {
					normalizedVal = -1;
				}
				Vote normalizedVote = new Vote(normalizedVal, vote.getVoterAgentId());
				result.add(normalizedVote);
			} catch (EnvelopeNotFoundException e) {
				break;
			} catch (Exception e) {
				// XXX logging
			}
		}
		return result;
	}

}
