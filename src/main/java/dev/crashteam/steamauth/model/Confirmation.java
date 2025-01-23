package dev.crashteam.steamauth.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Confirmation {

    @JsonProperty("id")
    private long id;

    @JsonProperty("nonce")
    private long key;

    @JsonProperty("creator_id")
    private long creator;

    @JsonProperty("headline")
    private String headline;

    @JsonProperty("summary")
    private List<String> summary;

    @JsonProperty("accept")
    private String accept;

    @JsonProperty("cancel")
    private String cancel;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("type")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private EMobileConfirmationType confType = EMobileConfirmationType.Invalid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getKey() {
        return key;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public long getCreator() {
        return creator;
    }

    public void setCreator(long creator) {
        this.creator = creator;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public List<String> getSummary() {
        return summary;
    }

    public void setSummary(List<String> summary) {
        this.summary = summary;
    }

    public String getAccept() {
        return accept;
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }

    public String getCancel() {
        return cancel;
    }

    public void setCancel(String cancel) {
        this.cancel = cancel;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public EMobileConfirmationType getConfType() {
        return confType;
    }

    public void setConfType(EMobileConfirmationType confType) {
        this.confType = confType;
    }

    public enum EMobileConfirmationType {
        Invalid(0),
        Test(1),
        Trade(2),
        MarketListing(3),
        FeatureOptOut(4),
        PhoneNumberChange(5),
        AccountRecovery(6);

        private final int code;

        EMobileConfirmationType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
