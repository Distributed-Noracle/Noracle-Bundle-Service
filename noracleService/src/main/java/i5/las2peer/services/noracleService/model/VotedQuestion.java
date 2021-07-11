package i5.las2peer.services.noracleService.model;

public class VotedQuestion extends Question {

	private static final long serialVersionUID = 1L;

	private VoteList votes;
	
	public VotedQuestion() { // used in tests
		
	}

	public VotedQuestion(Question q) {
		this.setQuestionId(q.getQuestionId());
		this.setText(q.getText());
		this.setSpaceId(q.getSpaceId());
		this.setAuthorId(q.getAuthorId());
		this.setTimestampCreated(q.getTimestampCreated());
		this.setTimestampLastModified(q.getTimestampLastModified());
		this.setDepth(q.getDepth());
	}
	
	public VoteList getVotes() {
		return votes;
	}

	public void setVotes(VoteList votes) {
		this.votes = votes;
	}

}
