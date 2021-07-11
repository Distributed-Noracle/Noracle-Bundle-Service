package i5.las2peer.services.noracleService.model;

import java.io.Serializable;

public class VoteEntry implements Serializable {

	private static final long serialVersionUID = 1L;

	private String objectId;
	private int pubIndex;
	private Vote vote;

	public VoteEntry(String objectId, int pubIndex, Vote vote) {
		this.objectId = objectId;
		this.pubIndex = pubIndex;
		this.vote = vote;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public int getPubIndex() {
		return pubIndex;
	}

	public void setPubIndex(int pubIndex) {
		this.pubIndex = pubIndex;
	}

	public Vote getVote() {
		return vote;
	}

	public void setVote(Vote vote) {
		this.vote = vote;
	}

}
