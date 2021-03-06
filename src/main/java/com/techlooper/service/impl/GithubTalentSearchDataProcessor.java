package com.techlooper.service.impl;

import com.techlooper.entity.userimport.UserImportEntity;
import com.techlooper.model.SocialProvider;
import com.techlooper.model.Talent;
import com.techlooper.model.TalentSearchRequest;
import com.techlooper.service.TalentSearchDataProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by NguyenDangKhoa on 3/11/15.
 */
@Service("GITHUBTalentSearchDataProcessor")
public class GithubTalentSearchDataProcessor implements TalentSearchDataProcessor {

    private final int PAGE_RESULT_THRESHOLD = 9;

    @Override
    public List<Talent> process(List<UserImportEntity> users) {
        return users.stream().map(userImportEntity -> {
            Map<String, Object> profile = (Map<String, Object>) userImportEntity.getProfiles().get(SocialProvider.GITHUB);

            if (profile == null) {
                return null;
            }

            Talent.Builder talentBuilder = new Talent.Builder();
            String username = StringUtils.trimToEmpty((String) profile.get("username"));
            String fullName = StringUtils.trimToEmpty((String) profile.get("fullName"));
            String description = StringUtils.trimToEmpty((String) profile.get("description"))
                    .replace(username, "")
                    .replace(" Follow their code on GitHub.", "")
                    .trim();
            String imageUrl = StringUtils.isNotEmpty((String) profile.get("imageurl")) ?
                    StringUtils.trimToEmpty((String) profile.get("imageurl")) : StringUtils.trimToEmpty((String) profile.get("imageUrl"));

            talentBuilder.withEmail(userImportEntity.getEmail())
                    .withUsername(username)
                    .withFullName(fullName)
                    .withImageUrl(imageUrl)
                    .withCompany(StringUtils.trimToEmpty((String) profile.get("company")))
                    .withDescription(description)
                    .withLocation(StringUtils.trimToEmpty((String) profile.get("location")))
                    .withJobTitle("")
                    .withSkills(((List<String>) profile.get("skills")))
                    .withScore(userImportEntity.getScore());
            return talentBuilder.build();
        }).filter(talent -> talent != null).collect(Collectors.toList());
    }

    @Override
    public void normalizeInputParameter(TalentSearchRequest param) {
        param.getSkills().removeAll(Arrays.asList(null, ""));
        param.getCompanies().removeAll(Arrays.asList(null, ""));
        param.getLocations().removeAll(Arrays.asList(null, ""));

        //process locations which contains whitespace among their words, remove the blank, "Ha Noi" -> "HaNoi"
        List<String> locations = param.getLocations();
        locations.addAll(locations.stream().filter(
                location -> location.contains(" ")).map(
                location -> new String(location.replaceAll(" ", ""))).collect(Collectors.toList()));
        //TODO : should define a map<keysearch,text search> for this later
        if (locations.contains("Ho Chi Minh")) {
            locations.add("saigon");
        }

        //limit the number of pages up to 10, zero-based page index
        if (param.getPageIndex() > PAGE_RESULT_THRESHOLD) {
            param.setPageIndex(PAGE_RESULT_THRESHOLD);
        }
    }

    @Override
    public TalentSearchRequest getSearchAllRequestParameter() {
        return new TalentSearchRequest();
    }

}
