package com.techlooper.service.impl;

import com.techlooper.entity.UserEntity;
import com.techlooper.entity.VnwUserProfile;
import com.techlooper.entity.userimport.UserImportEntity;
import com.techlooper.model.*;
import com.techlooper.repository.couchbase.UserRepository;
import com.techlooper.repository.talentsearch.query.TalentSearchQuery;
import com.techlooper.repository.userimport.UserImportRepository;
import com.techlooper.service.TalentSearchDataProcessor;
import com.techlooper.service.UserEvaluationService;
import com.techlooper.service.UserService;
import com.techlooper.service.VietnamWorksUserService;
import org.dozer.Mapper;
import org.elasticsearch.common.collect.Lists;
import org.jasypt.util.text.TextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.FacetedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

/**
 * Created by NguyenDangKhoa on 12/11/14.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Resource
    private UserRepository userRepository;

    @Resource
    private UserImportRepository userImportRepository;

    @Resource
    private Mapper dozerMapper;

    @Resource
    private TextEncryptor textEncryptor;

    @Resource
    private VietnamWorksUserService vietnamworksUserService;

    @Resource
    private UserEvaluationService userEvaluationService;

    @Resource
    private ApplicationContext applicationContext;

    public void save(UserEntity userEntity) {
        userRepository.save(userEntity);
    }

    public void save(UserInfo userInfo) {
        UserEntity userEntity = userRepository.findOne(userInfo.getId());
        dozerMapper.map(userInfo, userEntity);
        userRepository.save(userEntity);
    }

    public UserEntity findById(String id) {
        return userRepository.findOne(id);
    }

    public UserInfo findUserInfoByKey(String key) {
        return dozerMapper.map(findUserEntityByKey(key), UserInfo.class);
    }

    public UserEntity findUserEntityByKey(String key) {
        String emailAddress = textEncryptor.decrypt(key);
        return userRepository.findOne(emailAddress);
    }

    public boolean verifyVietnamworksAccount(UserEntity userEntity) {
        boolean result = vietnamworksUserService.existUser(userEntity.getEmailAddress());
        if (result) {
            userEntity.getProfiles().put(SocialProvider.VIETNAMWORKS, null);
        }
        return result;
    }

    public boolean registerVietnamworksAccount(UserInfo userInfo) {
        boolean registerSuccess = false;
        if (userInfo.acceptRegisterVietnamworksAccount() &&
                !(registerSuccess = vietnamworksUserService.register(dozerMapper.map(userInfo, VnwUserProfile.class)))) {
            userInfo.removeProfile(SocialProvider.VIETNAMWORKS);
        }
        return registerSuccess;
    }

    public boolean addCrawledUser(UserImportEntity userImportData, SocialProvider socialProvider) {
        UserImportEntity userImportEntity = findUserImportByEmail(userImportData.getEmail());

        if (userImportEntity != null) {
            userImportEntity.withProfile(socialProvider, userImportData.getProfiles().get(socialProvider));
        } else {
            userImportEntity = dozerMapper.map(userImportData, UserImportEntity.class);
            userImportEntity.withProfile(socialProvider, userImportData.getProfiles().get(socialProvider));
            userImportEntity.setCrawled(true);
        }

        return userImportRepository.save(userImportEntity) != null;
    }

    public int addCrawledUserAll(List<UserImportEntity> users, SocialProvider socialProvider) {
        List<UserImportEntity> shouldBeSavedUsers = new ArrayList<>();

        for (UserImportEntity user : users) {
            try {
                UserImportEntity userImportEntity = findUserImportByEmail(user.getEmail());
                if (userImportEntity != null) {
                    userImportEntity.withProfile(socialProvider, user.getProfiles().get(socialProvider));
                    shouldBeSavedUsers.add(userImportEntity);
                } else {
                    shouldBeSavedUsers.add(user);
                }
            } catch (Exception ex) {
                LOGGER.error("User Import Fail : " + user.getEmail(), ex);
            }
        }

        return Lists.newArrayList(userImportRepository.save(shouldBeSavedUsers)).size();
    }

    public UserImportEntity findUserImportByEmail(String email) {
        return userImportRepository.findOne(email);
    }

    @Override
    public List<UserImportEntity> getAll(int pageNumber, int pageSize) {
        final SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery()
                        .mustNot(wildcardQuery("email", "*users.noreply.github.com"))
                        .mustNot(wildcardQuery("email", "*missing.com")))
                .withPageable(new PageRequest(pageNumber, pageSize))
                .build();

        return userImportRepository.search(searchQuery).getContent();
    }

    @Override
    public TalentSearchResponse findTalent(final TalentSearchRequest param) {
        List<SocialProvider> socialProviders = Arrays.asList(SocialProvider.GITHUB);
        TalentSearchResponse.Builder builder = new TalentSearchResponse.Builder();

        socialProviders.forEach(provider -> {
            TalentSearchQuery talentSearchQuery =
                    applicationContext.getBean(provider + "TalentSearchQuery", TalentSearchQuery.class);
            ElasticsearchRepository talentSearchRepository =
                    applicationContext.getBean(provider + "TalentSearchRepository", ElasticsearchRepository.class);
            TalentSearchDataProcessor talentSearchDataProcessor =
                    applicationContext.getBean(provider + "TalentSearchDataProcessor", TalentSearchDataProcessor.class);

            talentSearchDataProcessor.normalizeInputParameter(param);
            FacetedPage<UserImportEntity> pageResult = talentSearchRepository.search(talentSearchQuery.getSearchQuery(param));
            builder.withTotal(pageResult.getTotalElements());
            builder.withResult(talentSearchDataProcessor.process(pageResult.getContent()));
        });

        return builder.build();
    }

    @Override
    public TalentProfile getTalentProfile(String email) {
        TalentProfile.Builder builder = new TalentProfile.Builder();
        UserImportEntity userImportEntity = findUserImportByEmail(email);
        Map<String,Long> skillMap = userEvaluationService.getSkillMap();
        Map<String, Object> profile = (Map<String, Object>) userImportEntity.getProfiles().get(SocialProvider.GITHUB);
        if (profile != null) {
            List<String> skills = (List<String>) profile.get("skills");
            Map<String,Long> talentSkillMap = new HashMap<>();
            skills.stream().forEach(skill -> talentSkillMap.put(skill, skillMap.get(skill.toLowerCase())));
            builder.withUserImportEntity(userImportEntity);
            builder.withSkillMap(talentSkillMap);
        }
        return builder.build();
    }

}
