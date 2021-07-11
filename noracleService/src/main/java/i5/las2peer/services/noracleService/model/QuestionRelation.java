package i5.las2peer.services.noracleService.model;

import java.io.Serializable;

public class QuestionRelation implements Serializable {

	private static final long serialVersionUID = 1L;

	private String relationId;
	private String spaceId;
	private String authorId;
	private String name;
	private String firstQuestionId;
	private String secondQuestionId;
	private boolean directed;
	private String timestampCreated;
	private String timestampLastModified;

	public QuestionRelation() { // used in tests
	}

	public QuestionRelation(String relationId, String spaceId, String authorId, String name, String firstQuestionId,
			String secondQuestionId, Boolean directed, String timestampCreated) {
		this.relationId = relationId;
		this.spaceId = spaceId;
		this.authorId = authorId;
		this.name = name;
		this.firstQuestionId = firstQuestionId;
		this.secondQuestionId = secondQuestionId;
		this.directed = directed == null ? false : directed;
		this.timestampCreated = timestampCreated;
		this.timestampLastModified = timestampCreated;
	}

	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}

	public String getRelationId() {
		return relationId;
	}

	public void setRelationId(String relationId) {
		this.relationId = relationId;
	}

	public String getSpaceId() {
		return spaceId;
	}

	public String getAuthorId() {
		return authorId;
	}

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

	public boolean isDirected() {
		return directed;
	}

	public void setDirected(boolean directed) {
		this.directed = directed;
	}

	public String getTimestampCreated() {
		return timestampCreated;
	}

	public void setTimestampCreated(String timestampCreated) {
		this.timestampCreated = timestampCreated;
	}

	public String getTimestampLastModified() {
		return timestampLastModified;
	}

	public void setTimestampLastModified(String timestampLastModified) {
		this.timestampLastModified = timestampLastModified;
	}

	public void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

}
