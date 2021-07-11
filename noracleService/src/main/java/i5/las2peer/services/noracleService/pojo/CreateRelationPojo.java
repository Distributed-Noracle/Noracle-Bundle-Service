package i5.las2peer.services.noracleService.pojo;

public class CreateRelationPojo {

	private String name;
	private String firstQuestionId;
	private String secondQuestionId;
	private Boolean directed;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFirstQuestionId() {
		return firstQuestionId;
	}

	public void setFirstQuestionId(String firstQuestionId) {
		this.firstQuestionId = firstQuestionId;
	}

	public String getSecondQuestionId() {
		return secondQuestionId;
	}

	public void setSecondQuestionId(String secondQuestionId) {
		this.secondQuestionId = secondQuestionId;
	}

	public Boolean isDirected() {
		return directed;
	}

	public void setDirected(Boolean directed) {
		this.directed = directed;
	}

}
