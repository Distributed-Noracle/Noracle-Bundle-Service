package i5.las2peer.services.noracleService.pojo;

public class ChangeQuestionRelationPojo {

	private String name;
	private String questionId1;
	private String questionId2;
	private Boolean directed;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getQuestionId1() {
		return questionId1;
	}

	public void setQuestionId1(String questionId1) {
		this.questionId1 = questionId1;
	}

	public String getQuestionId2() {
		return questionId2;
	}

	public void setQuestionId2(String questionId2) {
		this.questionId2 = questionId2;
	}

	public Boolean getDirected() {
		return directed;
	}

	public void setDirected(Boolean directed) {
		this.directed = directed;
	}

}
