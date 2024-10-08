package net.mcczai.cardduel.client.resource.pojo;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PackInfoPOJO {
    @SerializedName("version")
    private String version = "1.0.0";

    @SerializedName("name")
    private String name = "custom.cardduel.err.no_name";

    @SerializedName("desc")
    private String description = "";

    @SerializedName("license")
    private String license = "All Rights Reserved";

    @SerializedName("authors")
    private List<String> authors = Lists.newArrayList();

    @SerializedName("date")
    private String date = "1919-08-10";


    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLicense() {
        return license;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getDate() {
        return date;
    }

}
