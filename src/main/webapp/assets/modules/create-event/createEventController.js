techlooper.controller("createEventController", function ($scope, $translate, jsonValue, apiService, $rootScope, utils,
                                                         $location, $anchorScroll, localStorageService) {

  $scope.createWebinar = function () {
    $anchorScroll();
    utils.sendNotification(jsonValue.notifications.loading, $(window).height());
    $scope.webinarForm.$setSubmitted();
    if ($scope.webinarForm.$invalid) {
      utils.sendNotification(jsonValue.notifications.loaded);
      return;
    }

    var webinar = $scope.webinar;
    webinar.lang = $translate.use();
    apiService.createWebinar(webinar)
      .success(function (data) {
        var title = utils.toAscii(data.name);
        var path = sprintf("event-detail/%s-%s-id", title, data.createdDateTime);
        localStorageService.set("webinarCreated", true);
        $location.path(path);
      })
      .finally(function () {
        utils.sendNotification(jsonValue.notifications.loaded);
      });

    $scope.webinarForm.$setPristine();
  }

  $scope.uiConfig = {
    attendantsConfig: {
      type: "email",
      placeholder: "attendantsEx",
      showCurrentUserEmail: true
    },

    fromNowDatetimeConfig: {
      step: 10,
      minDate: new Date()
    }
  }

  //$scope.webinar = {
  //  attendees: [$rootScope.userInfo.email]
  //}

  $scope.state = function (type) {
    switch (type) {
      case "error-event-date":
        return $scope.webinarForm.$submitted || $scope.webinarForm.startDate.$dirty || $scope.webinarForm.endDate.$dirty;

      case "error-required-event-date":
        if ($scope.webinarForm.startDate.$error.required) {
          return true;
        }
        if (!$scope.webinarForm.endDate.$dirty) return false;
        if ($scope.webinarForm.endDate.$error.required) {
          return true;
        }
        return false;

      case "error-past-date":
        if (!$scope.webinar) return false;
        $scope.webinarForm.startDate.$setValidity("past", true);
        $scope.webinarForm.endDate.$setValidity("past", true);

        var isPast = false;

        if ($scope.webinar.startDate) {
          var startDate = moment($scope.webinar.startDate, jsonValue.dateTimeFormat);
          isPast |= startDate.isBefore(moment()) || startDate.isBefore(moment());
          $scope.webinarForm.startDate.$setValidity("past", !isPast);
        }

        if ($scope.webinar.endDate) {
          var endDate = moment($scope.webinar.endDate, jsonValue.dateTimeFormat);
          isPast |= endDate.isBefore(moment()) || endDate.isBefore(moment());
          $scope.webinarForm.endDate.$setValidity("past", !isPast);
        }

        return isPast;

      case "error-range-date":
        if (!$scope.webinar) return false;
        if ($scope.state("error-past-date")) return false;
        $scope.webinarForm.startDate.$setValidity("range", true);
        $scope.webinarForm.endDate.$setValidity("range", true);

        if ($scope.webinar.startDate && $scope.webinar.endDate) {
          var startDate = moment($scope.webinar.startDate, jsonValue.dateTimeFormat);
          var endDate = moment($scope.webinar.endDate, jsonValue.dateTimeFormat);
          var isEndDateBeforeStartDate = endDate.isBefore(startDate);
          $scope.webinarForm.startDate.$setValidity("range", !isEndDateBeforeStartDate);
          $scope.webinarForm.endDate.$setValidity("range", !isEndDateBeforeStartDate);
          return isEndDateBeforeStartDate;
        }
        return false;
    }

    return false;
  }

});