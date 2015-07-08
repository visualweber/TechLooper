techlooper.controller('contestsController', function (apiService, $scope, jsonValue, $window, $translate, $filter) {
  $scope.contestTimeLeft = function(contest) {
    switch (contest.progress.translate) {
      case jsonValue.status.progress.translate:
        return $filter("countdown")(contest.submissionDateTime);

      case jsonValue.status.notStarted.translate:
        return $filter("countdown")(contest.startDateTime);

      case jsonValue.status.registration.translate:
        return $filter("countdown")(contest.registrationDateTime);

      case jsonValue.status.closed.translate:
        return contest.submissionDateTime;
    }

    return "";
  }

  apiService.searchContests().success(function(contests) {
    $scope.contestsList = contests;
  });

});