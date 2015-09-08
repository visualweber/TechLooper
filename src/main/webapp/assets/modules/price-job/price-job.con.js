techlooper.controller("priceJobController", function ($scope, $rootScope, jsonValue, $http, utils, $translate, $route, validatorService, vnwConfigService) {

  var jobLevels = $.extend(true, [], jsonValue.jobLevels.filter(function (value) {return value.id > 0;}));

  $scope.$watch("translate", function () {
    if ($rootScope.translate === undefined) {
      return;
    }

    var translate = $rootScope.translate;
    $.each(jobLevels, function (i, jobLevel) {jobLevel.translate = translate[jobLevel.translate];});

    $.each([
      {item: "jobLevels", translate: "exManager"}
    ], function (i, select) {
      if (!$scope.selectize[select.item].$elem) {
        return true;
      }
      $scope.selectize[select.item].$elem.settings.placeholder = translate[select.translate];
      $scope.selectize[select.item].$elem.updatePlaceholder();
    });
  });

  $scope.locationsConfig = vnwConfigService.locationsSelectize;
  $scope.industriesConfig = vnwConfigService.industriesSelectize;
  $scope.educationLevelConfig = vnwConfigService.educationLevel;
  $scope.companySizeConfig = vnwConfigService.companySizeSelectize;
  $scope.yearsOfExperienceConfig = vnwConfigService.yearsOfExperience;
  $scope.languagesConfig = vnwConfigService.languagesSelectize;
  $scope.selectize = {
    jobLevels: {
      items: jobLevels,
      config: {
        valueField: 'id',
        labelField: 'translate',
        delimiter: '|',
        maxItems: 1,
        searchField: ['translate'],
        placeholder: $translate.instant("exManager"),
        onInitialize: function (selectize) {
          $scope.selectize.jobLevels.$elem = selectize;
        }
      }
    }
  }

  $scope.selectedTime = $translate.instant("day");
  $scope.error = {};
  $scope.priceJob = {
    jobCategories: [],
    companySizeId: '',
    locationId: '',
    jobTitle: '',
    jobLevelIds: [],
    skills: [],
    languagesId: [],
    yearsExperienceId: '',
    educationRequiredId: ''
  };

  $scope.removeSkill = function (skill) {
    $scope.priceJob.skills.splice($scope.priceJob.skills.indexOf(skill), 1);
    $scope.error = {};
  }

  $scope.addNewSkill = function () {
    $scope.priceJob.skills || ($scope.priceJob.skills = []);
    if ($scope.newSkillName === undefined) {
      return;
    }
    var newSkillName = $scope.newSkillName.trim();
    $scope.newSkillName = newSkillName;
    if (newSkillName.length == 0) {
      $scope.error = {};
      return;
    }

    if ($scope.priceJob.skills.length === 3) {
      var translate = $rootScope.translate;
      $scope.error.newSkillName = translate.maximum3;
      return;
    }
    delete $scope.error.newSkillName;

    var skillLowerCases = $scope.priceJob.skills.map(function (skill) {return skill.toLowerCase();});
    if (skillLowerCases.indexOf(newSkillName.toLowerCase()) >= 0) {
      var translate = $rootScope.translate;
      $scope.error.existSkillName = translate.hasExist;
      return;
    }
    delete $scope.error.existSkillName;

    if (newSkillName.length > 0) {
      $scope.priceJob.skills.push(newSkillName);
      $scope.newSkillName = "";
      $("#txtTopSkills").focus();
    }
  }

  $scope.step = "step1";
  $scope.validate = function(){
    var inputs = $(".price-job-step-content").find("div:visible").find("[ng-model]");
    $.each(inputs, function (i, input) {
      var modelName = $(input).attr("ng-model");
      if (modelName === 'newSkillName' || $(input).attr("name") === undefined) {
        return true;
      }
      delete $scope.error[modelName];
      var inputValue = $scope.$eval(modelName);
      var notHasValue = ($.type(inputValue) === "array") && (inputValue.length === 0);
      notHasValue = notHasValue || !inputValue;
      notHasValue = notHasValue || (inputValue.length <= 0);
      notHasValue && ($scope.error[modelName] = $rootScope.translate.requiredThisField);
    });
    if($scope.step == "step2"){
      $scope.priceJob.skills.length || ($scope.error.skills = $rootScope.translate.requiredThisField);
      $scope.priceJob.skills.length && (delete $scope.error.skills);
    }
    var error = $.extend(true, {}, $scope.error);
    delete error.existSkillName;
    delete error.newSkillName;
    return $.isEmptyObject(error);
  }

  $scope.nextStep = function (step, priorStep) {
    if ((($scope.step === priorStep || step === "step3") && !$scope.validate()) || $scope.step === "step3") {
      return;
    }
    var swstep = step || $scope.step;
    $scope.step = swstep;

    switch (swstep) {
      case "step3":
        console.log($scope.priceJob);
        var priceJob = $.extend(true, {}, $scope.priceJob);
        priceJob.jobLevelIds = jsonValue.jobLevelsMap[priceJob.jobLevelIds].ids;
        priceJob.yearsExperienceId = jsonValue.yearsOfExperienceMap[priceJob.yearsExperienceId].ids;
        //priceJob.yearsExperienceId = vnwConfigService.experiences.map[];
        console.log(priceJob);
        utils.sendNotification(jsonValue.notifications.switchScope);
        $http.post("priceJob", priceJob)
          .success(function (data, status, headers, config) {
            $scope.priceJob = data;
            $scope.priceJob.jobCategoryLabels = $scope.priceJob.jobCategories.map(function(cat) {
              return jsonValue.industries[cat].value;
            });
            utils.sendNotification(jsonValue.notifications.loaded);
          });
        break;
    }
  }
  $scope.createNewReport = function () {
    $route.reload();
  }

  $scope.submitSurvey = function() {
    $scope.error = validatorService.validate($(".salary-report-feedback-form").find("input"));
    $scope.error = $.extend(true, $scope.error, validatorService.validate($(".salary-report-feedback-form").find("input")));

    $scope.survey.priceJobId = $scope.priceJob.createdDateTime;
    $http.post("savePriceJobSurvey", $scope.survey)
      .success(function() {
        $scope.survey.saved = true;
      })
  }
  $scope.removeBoxContent = function (cls) {
    //$scope.survey = {closed: true};
    $('.' + cls).slideUp("normal", function () { $(this).remove(); });
  }
});