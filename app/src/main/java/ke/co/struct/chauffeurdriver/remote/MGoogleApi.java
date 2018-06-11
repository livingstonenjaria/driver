package ke.co.struct.chauffeurdriver.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface MGoogleApi {
    @GET
    Call<String> getPath(@Url String url);
}
