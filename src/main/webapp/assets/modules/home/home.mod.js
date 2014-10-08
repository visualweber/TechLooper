angular.module("Home").directive("chart", function() {
   return {
      restrict : "A", // This mens that it will be used as an attribute and NOT as an element.
      replace : false,
      templateUrl : "modules/collection/chart.tpl.html",
      controller : "chartController"
   }
}).directive("findjobs", function() {
   return {
      restrict : "A", // This mens that it will be used as an attribute and NOT as an element.
      replace : true,
      templateUrl : "modules/job/findJobs.tpl.html",
      controller: 'findJobsController'
   }
});
