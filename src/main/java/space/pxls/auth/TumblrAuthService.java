package space.pxls.auth;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import space.pxls.App;

public class TumblrAuthService extends AuthService {
    public TumblrAuthService(String id) {
        super(id, App.getConfig().getBoolean("oauth.tumblr.enabled"),
                App.getConfig().getBoolean("oauth.tumblr.registrationEnabled"));
    }

    @Override
    public String getRedirectUrl(String state) {
        return "https://www.tumblr.com/oauth2/authorize?" +
                "scope=basic&" +
                "state=" + state + "&" +
                "redirect_uri=" + getCallbackUrl() + "&" +
                "response_type=code&" +
                "client_id=" + App.getConfig().getString("oauth.tumblr.key");
    }

    @Override
    public String getToken(String code) throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.post("https://api.tumblr.com/v2/oauth2/token")
                .header("User-Agent", "pxls.space")
                .field("grant_type", "authorization_code")
                .field("code", code)
                .field("redirect_uri", getCallbackUrl())
                .field("client_id", App.getConfig().getString("oauth.tumblr.key"))
                .field("client_secret", App.getConfig().getString("oauth.tumblr.secret"))
                .asJson();

        JSONObject json = response.getBody().getObject();

        if (json.has("error")) {
            return null;
        } else {
            return json.getString("access_token");
        }
    }

    @Override
    public String getIdentifier(String token) throws UnirestException {
        HttpResponse<JsonNode> me = Unirest.get("https://api.tumblr.com/v2/user/info")
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "pxls.space")
                .asJson();
        JSONObject json = me.getBody().getObject();
        if (json.has("error")) {
            return null;
        } else {
            try {
                return json.getJSONObject("response").getJSONObject("user").getString("name");
            } catch (JSONException e) {
                return null;
            }
        }
    }

    public String getName() {
        return "Tumblr";
    }

    @Override
    public void reloadEnabledState() {
        this.enabled = App.getConfig().getBoolean("oauth.tumblr.enabled");
        this.registrationEnabled = App.getConfig().getBoolean("oauth.tumblr.registrationEnabled");
    }
}
