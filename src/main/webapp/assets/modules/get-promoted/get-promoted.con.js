techlooper.controller('getPromotedController', function ($scope, utils, vnwConfigService, $q, userPromotionService, $http, $location) {
  $scope.selectize = vnwConfigService;

  //var formSubmitted = function(form, prop) {
  //  return form && (form[prop].$touched || form.$submitted);
  //}

  var state = {
    default: {
      showView: function (viewName) {
        switch (viewName) {
          case "enable-do-suggestion":
            return true;
        }
      }
    },

    result: {
      showResult: true,

      showView: function (viewName) {
        var promotionResult = $scope.masterPromotion.result;
        var promotionEmailForm = $scope.promotionEmailForm;
        var promotionSurveyForm = $scope.promotionSurveyForm;
        var hasPromotionResult = promotionResult && $.type(promotionResult.salaryMin) === "number" && $.type(promotionResult.salaryMax) === "number";
        var surveyFormHasSubmitted = utils.isFormSubmitted($scope.promotionSurveyForm, "isUnderstandable");

        var errorRequired, sentSurvey, sentEmail;
        switch (viewName) {
          case "error-email-is-submitted":
            return utils.isFormSubmitted($scope.promotionEmailForm, "promotionEmail");

          case "error-required-survey-is-understandable":
            errorRequired = $scope.promotionSurveyForm.isUnderstandable.$error.required;
            return errorRequired && surveyFormHasSubmitted;

          case "error-required-survey-is-accurate":
            errorRequired = $scope.promotionSurveyForm.isAccurate.$error.required;
            return errorRequired && surveyFormHasSubmitted;

          case "error-required-survey-is-learn-more":
            errorRequired = $scope.promotionSurveyForm.wantToLearnMore.$error.required;
            return errorRequired && surveyFormHasSubmitted;

          case "no-promotion-result":
            return !hasPromotionResult;

          case "has-promotion-result":
            return hasPromotionResult;

          case "sent-promotion-email-no-result":
            sentEmail = promotionEmailForm && promotionEmailForm.$sentEmail;
            return !hasPromotionResult && sentEmail;

          case "sent-promotion-email-has-result":
            sentEmail = promotionEmailForm && promotionEmailForm.$sentEmail;
            return hasPromotionResult && sentEmail;

          case "not-sent-promotion-email":
            sentEmail = promotionEmailForm && promotionEmailForm.$sentEmail;
            return !sentEmail;

          case "sent-promotion-survey":
            sentSurvey = promotionSurveyForm && promotionSurveyForm.$sentSurvey;
            return sentSurvey;

          case "not-sent-promotion-survey":
            sentSurvey = promotionSurveyForm && promotionSurveyForm.$sentSurvey;
            return !sentSurvey;

          case "user-promotion-survey-form":
            return hasPromotionResult;
        }
        return false;
      }
    }
  }

  $scope.viewsDefers = {
    getPromotedForm: $q.defer()
  };

  var doPromotionWithParam = function (promotionInfo, ignoreValidation) {
    $scope.promotionInfo = angular.copy(userPromotionService.refinePromotionInfo(promotionInfo));
    $scope.doPromotion(ignoreValidation);
  }

  //var viewsPromises = utils.toPromises($scope.viewsDefers);
  $q.all($scope.viewsDefers.getPromotedForm).then(function (data) {
    //http://localhost:8080/#/get-promoted?jobTitle=java&jobLevelIds=[5,6]&jobCategoryIds=[35,55,57]&lang=en&utm_source=getpromotedemail&utm_medium=skilltrendsbutton&utm_campaign=howtogetpromoted
    var param = $location.search();
    if (param.id) {
      $http.get("getPromotedResult/" + param.id).success(function (data, status, headers, config) {
        doPromotionWithParam(data, true);
      });
    }
    else if (!$.isEmptyObject(param)) {
      param = utils.toObject(param);
      doPromotionWithParam(param);
    }
  });

  $scope.changeState = function (st) {
    var pState = angular.copy(state[st] || st);
    $scope.state = pState;
    $scope.$emit("$stateChangeSuccess");
  }

  $scope.changeState("default");
});