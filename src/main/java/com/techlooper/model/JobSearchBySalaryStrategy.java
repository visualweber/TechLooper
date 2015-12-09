package com.techlooper.model;

import com.techlooper.entity.JobEntity;
import com.techlooper.util.DataUtils;
import org.elasticsearch.index.query.*;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * Created by NguyenDangKhoa on 12/7/15.
 */
public class JobSearchBySalaryStrategy extends JobSearchStrategy {

    private SalaryReviewDto salaryReviewDto;

    private ElasticsearchRepository<JobEntity, ?> repository;

    public JobSearchBySalaryStrategy(SalaryReviewDto salaryReviewDto, ElasticsearchRepository elasticsearchRepository) {
        this.salaryReviewDto = salaryReviewDto;
        this.repository = elasticsearchRepository;
    }

    @Override
    protected NativeSearchQueryBuilder getSearchQueryBuilder() {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withTypes("job");

        //pre-process job title in case user enters multiple roles of his job
        List<String> jobTitleTokens = DataUtils.preprocessJobTitle(salaryReviewDto.getJobTitle());

        BoolQueryBuilder boolQueryBuilder = boolQuery();
        jobTitleTokens.forEach(jobTitleToken -> boolQueryBuilder.should(jobTitleQueryBuilder(jobTitleToken.trim())));
        FilterBuilder jobIndustriesFilterBuilder = getJobIndustriesFilterBuilder(salaryReviewDto.getJobCategories());
        FilterBuilder approvedDateRangeFilterBuilder = getRangeFilterBuilder("approvedDate", "now-6M/M", null);
        FilterBuilder salaryRangeFilterBuilder = getSalaryRangeFilterBuilder(MIN_SALARY_ACCEPTABLE, MAX_SALARY_ACCEPTABLE);

        queryBuilder.withQuery(filteredQuery(boolQueryBuilder,
                boolFilter().must(approvedDateRangeFilterBuilder)
                        .must(jobIndustriesFilterBuilder)
                        .must(salaryRangeFilterBuilder)));
        return queryBuilder;
    }

    @Override
    protected ElasticsearchRepository<JobEntity, ?> getJobRepository() {
        return repository;
    }

}