package i5.las2peer.services.noracleService.model;

import java.io.Serializable;

public class SpaceSubscription implements Serializable {

	private static final long serialVersionUID = 1L;

	private String spaceId;
	private String[] selectedQuestionIds;

	public SpaceSubscription() { // used in tests
	}

	public SpaceSubscription(String spaceId, String name) {
		this.setSpaceId(spaceId);
		this.setSelectedQuestionIds(new String[0]);
	}

	public String getSpaceId() {
		return spaceId;
	}

	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}

	public String[] getSelectedQuestionIds() {
		return selectedQuestionIds;
	}

	public void setSelectedQuestionIds(String[] selectedQuestionIds) {
		this.selectedQuestionIds = selectedQuestionIds;
	}

}
