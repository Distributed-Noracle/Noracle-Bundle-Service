package i5.las2peer.services.noracleService.model;

import java.io.Serializable;

public class Space implements Serializable {

	private static final long serialVersionUID = 1L;

	private String spaceId;
	private String spaceSecret;
	private String name;
	private String spaceOwnerId;
	private String spaceReaderGroupId;

	public Space() { // used in tests
	}

	public Space(String spaceId, String spaceSecret, String name, String spaceOwnerId, String spaceReaderGroupId) {
		this.setSpaceId(spaceId);
		this.setSpaceSecret(spaceSecret);
		this.setName(name);
		this.setSpaceOwnerId(spaceOwnerId);
		this.setSpaceReaderGroupId(spaceReaderGroupId);
	}

	public String getSpaceId() {
		return spaceId;
	}

	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}

	public String getSpaceSecret() {
		return spaceSecret;
	}

	public void setSpaceSecret(String spaceSecret) {
		this.spaceSecret = spaceSecret;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSpaceOwnerId() {
		return spaceOwnerId;
	}

	public void setSpaceOwnerId(String spaceOwnerId) {
		this.spaceOwnerId = spaceOwnerId;
	}

	public String getSpaceReaderGroupId() {
		return spaceReaderGroupId;
	}

	public void setSpaceReaderGroupId(String spaceReaderGroupId) {
		this.spaceReaderGroupId = spaceReaderGroupId;
	}

}
