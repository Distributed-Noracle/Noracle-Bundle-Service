package i5.las2peer.services.noracleService.pojo;

import java.io.Serializable;

public class SubscribeSpacePojo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String spaceId;
	private String spaceSecret;

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

}
