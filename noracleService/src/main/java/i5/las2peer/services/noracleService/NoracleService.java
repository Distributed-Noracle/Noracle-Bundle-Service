package i5.las2peer.services.noracleService;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.noracleService.resources.AgentsResource;
import i5.las2peer.services.noracleService.resources.QuestionRelationsResource;
import i5.las2peer.services.noracleService.resources.QuestionsResource;
import i5.las2peer.services.noracleService.resources.SpacesResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

import javax.ws.rs.Path;

@Api
@SwaggerDefinition(
		info = @Info(
				title = "Noracle Service",
				version = NoracleService.API_VERSION,
				description = "A bundle service for the distributed Noracle system",
				license = @License(
						name = "BSD-3",
						url = "https://github.com/Distributed-Noracle/Distributed-Noracle-Backend/blob/master/LICENSE.txt")))
@ServicePath("/" + NoracleService.RESOURCE_NAME)
public class NoracleService extends RESTService {

	public static final String RESOURCE_NAME = "distributed-noracle";
	public static final String API_VERSION = "0.7";

	@Override
	protected void initResources() {
		getResourceConfig().register(this.getClass());
		getResourceConfig().register(SpacesResource.class);
		getResourceConfig().register(QuestionsResource.class);
		getResourceConfig().register(QuestionRelationsResource.class);
		getResourceConfig().register(AgentsResource.class);
	}

	@Path("/" + SpacesResource.RESOURCE_NAME)
	public SpacesResource spaces() {
		return new SpacesResource();
	}

	@Path("/" + AgentsResource.RESOURCE_NAME + "/{agentid}")
	public AgentsResource agents() {
		return new AgentsResource();
	}

}
