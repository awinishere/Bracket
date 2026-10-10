package com.awn.bracket.bucket.adapters.http;

import com.awn.bracket.bucket.adapters.http.dto.BucketResponse;
import com.awn.bracket.bucket.adapters.http.dto.CreateBucketRequest;
import com.awn.bracket.bucket.domain.Bucket;
import com.awn.bracket.bucket.ports.in.CreateBucketUseCase;
import com.awn.bracket.bucket.ports.in.DeleteBucketUseCase;
import com.awn.bracket.bucket.ports.in.FindBucketByIdUseCase;
import com.awn.bracket.bucket.ports.in.FindBucketByNameUseCase;
import com.awn.bracket.bucket.ports.in.ListBucketsUseCase;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/buckets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BucketResource {

    private static final Logger LOG = Logger.getLogger(BucketResource.class);

    @Inject
    CreateBucketUseCase createBucket;

    @Inject
    FindBucketByIdUseCase findBucketById;

    @Inject
    FindBucketByNameUseCase findBucketByName;

    @Inject
    ListBucketsUseCase listBuckets;

    @Inject
    DeleteBucketUseCase deleteBucket;

    @POST
    public Response create(@Valid CreateBucketRequest req) {
        Bucket saved = createBucket.create(req.name(), req.visibility(), req.region());
        URI location = URI.create("/api/v1/buckets/" + saved.id());
        return Response.created(location).entity(BucketResponse.from(saved)).build();
    }

    @GET
    public List<BucketResponse> list() {
        return listBuckets.list().stream().map(BucketResponse::from).toList();
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public BucketResponse getById(@PathParam("id") UUID id) {
        return findBucketById.find(id)
                .map(BucketResponse::from)
                .orElseThrow(() -> new BucketNotFoundException("bucket " + id + " not found"));
    }

    @GET
    @Path("/{name}")
    public BucketResponse getByName(@PathParam("name") String name) {
        return findBucketByName.find(name)
                .map(BucketResponse::from)
                .orElseThrow(() -> new BucketNotFoundException("bucket '" + name + "' not found"));
    }

    @DELETE
    @Path("/{id}")
    public Response deleteById(@PathParam("id") UUID id) {
        boolean removed = deleteBucket.delete(id);
        if (!removed) {
            throw new BucketNotFoundException("bucket " + id + " not found");
        }
        LOG.debugf("deleted bucket %s", id);
        return Response.noContent().build();
    }

    public static class BucketNotFoundException extends jakarta.ws.rs.WebApplicationException {
        public BucketNotFoundException(String message) {
            super(Response.status(Response.Status.NOT_FOUND)
                          .entity(Map.of("error", message))
                          .build());
        }
    }
}
