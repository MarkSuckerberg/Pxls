package space.pxls.auth;

import java.util.ArrayList;
import java.util.List;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import space.pxls.App;
import space.pxls.user.Role;

public class OidcAuthService extends AuthService {
    private String endpointAutorization;
    private String endpointToken;
    private String endpointUserinfo;

    public OidcAuthService(String id) {
        super(id, App.getConfig().getBoolean("oauth.oidc.enabled"),
                App.getConfig().getBoolean("oauth.oidc.registrationEnabled"));
        if (enabled) {
            discoverEndpoints();
        }
    }

    private void discoverEndpoints() {
        HttpResponse<JsonNode> httpResponse = Unirest.get(App.getConfig().getString("oauth.oidc.path"))
                .header("User-Agent", "pxls.space")
                .asJson();

        JSONObject jsonObject = httpResponse.getBody().getObject();

        endpointAutorization = jsonObject.getString("authorization_endpoint");
        endpointToken = jsonObject.getString("token_endpoint");
        endpointUserinfo = jsonObject.getString("userinfo_endpoint");
    }

    @Override
    public String getRedirectUrl(String state) {
        return endpointAutorization + "?client_id=" + App.getConfig().getString("oauth.oidc.key")
                + "&redirect_uri=" + getCallbackUrl() + "&response_type=code&scope=openid%20profile&state="
                + state;
    }

    @Override
    public String getToken(String code) throws UnirestException {
        HttpResponse<JsonNode> httpResponse = Unirest.post(endpointToken)
                .header("User-Agent", "pxls.space")
                .field("client_id", App.getConfig().getString("oauth.oidc.key"))
                .field("client_secret", App.getConfig().getString("oauth.oidc.secret"))
                .field("code", code).field("grant_type", "authorization_code")
                .field("redirect_uri", getCallbackUrl())
                .asJson();

        JSONObject jsonObject = httpResponse.getBody().getObject();

        if (!jsonObject.has("access_token")) {
            return null;
        }

        return jsonObject.getString("access_token");
    }

    @Override
    public String getIdentifier(String token) throws UnirestException, InvalidAccountException {
        HttpResponse<JsonNode> httpResponse = Unirest.get(endpointUserinfo)
                .header("User-Agent", "pxls.space")
                .header("Authorization", "Bearer " + token)
                .header("Client-Id", App.getConfig().getString("oauth.oidc.key"))
                .asJson();

        if (!httpResponse.isSuccess()) {
            return null;
        }

        return httpResponse.getBody().getObject().getString("sub");
    }

    @Override
    public List<Role> getRoles(String token) {
        HttpResponse<JsonNode> httpResponse = Unirest.get(endpointUserinfo)
                .header("User-Agent", "pxls.space")
                .header("Authorization", "Bearer " + token)
                .header("Client-Id", App.getConfig().getString("oauth.oidc.key"))
                .asJson();

        if (!httpResponse.isSuccess()) {
            return null;
        }

        JSONArray rawArray = httpResponse.getBody().getObject()
                .optJSONArray(App.getConfig().getString("oauth.oidc.roles"));

        List<Role> roleClaims = new ArrayList<>();
        for (int i = 0; i < rawArray.length(); i++) {
            String claim = rawArray.getString(i);
            Role roleClaim = Role.fromID(claim);

            if (roleClaim == null) {
                continue;
            }

            roleClaims.add(roleClaim);
        }

        return roleClaims;
    }

    @Override
    public String getName() {
        return App.getConfig().getString("oauth.oidc.name");
    }

    @Override
    public void reloadEnabledState() {
        this.enabled = App.getConfig().getBoolean("oauth.oidc.enabled");
        this.registrationEnabled = App.getConfig().getBoolean("oauth.oidc.registrationEnabled");
        if (enabled) {
            discoverEndpoints();
        }
    }
}