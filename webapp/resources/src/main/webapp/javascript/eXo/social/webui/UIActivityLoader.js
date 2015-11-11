(function ($){
  var UIActivityLoader = {
    delta : 65,
    responsiveId : null,
    numberOfReqsPerSec : 10,//Perfect range: 5 -> 20
    hasMore: false,
    parentContainer : $('#UIActivitiesLoader'),
    activitiesDisplay : $('.uiActivitiesDisplay:first'),
    portletId: null,
    scrollBottom : function() {
    return $(document).height() - $(window).scrollTop() - $(window).height();  
    },
    init: function (parentId, hasMore) {
      UIActivityLoader.portletId = parentId;
      UIActivityLoader.hasMore = (hasMore === true || hasMore === 'true') ? true : false;
      UIActivityLoader.initIndicator();

      $(document).ready(function() {
        // check onLoad page.
        if(UIActivityLoader.scrollBottom() <= UIActivityLoader.delta) {
          $(window).scrollTop($(document).height() - $(window).height() - (UIActivityLoader.delta+1));
        }
        UIActivityLoader.processBottomTimeLine();
      });

      UIActivityLoader.responsiveUI();
      

    },
    responsiveUI: function() {
      var deviceInfo = eXo.social.SocialUtil.checkDevice();
      if(deviceInfo.isMobile === true || deviceInfo.isTablet === true ||  deviceInfo.isTabletL === true  ) {
        UIActivityLoader.parentContainer = $('#UIActivitiesLoader');
        UIActivityLoader.activitiesDisplay = $('.uiActivitiesDisplay:first');
        UIActivityLoader.resetRightHeight();

        UIActivityLoader.parentContainer.parent().find('.changeStatus').off('click').click(function(evt) {
          $.publish("exo_social_composer_show");
        });
        //TODO moved to SocialUtils.js and using each to remove or add class
        $.subscribe("exo_social_activityStream_responsive_hide", function() {
          UIActivityLoader.activitiesDisplay.addClass('hidden-phone');
          //hide activity loader
          var activityLoadMore = UIActivityLoader.activitiesDisplay.find('#ActivitiesLoader');
          if (activityLoadMore != null) {
             activityLoadMore.hide();      
          }
        });

        $.subscribe("exo_social_activityStream_responsive_hide_top", function() {
          UIActivityLoader.activitiesDisplay.find('.activityTop').addClass('hidden-phone');
          //hide activity loader
          var activityLoadMore = UIActivityLoader.activitiesDisplay.find('#ActivitiesLoader');
          if (activityLoadMore != null) {
             activityLoadMore.hide();      
          }
        });

        $.subscribe("exo_social_activityStream_responsive_show", function() {
          UIActivityLoader.activitiesDisplay.find('.activityTop').removeClass('hidden-phone');
          UIActivityLoader.activitiesDisplay.removeClass('hidden-phone');

          var activityLoadMore = UIActivityLoader.activitiesDisplay.find('#ActivitiesLoader');
          if (activityLoadMore != null) {
             activityLoadMore.show();      
          }
        });

        $.subscribe("exo_social_activity_view", UIActivityLoader.onViewActivity);

        $.subscribe("exo_social_activityStream_responsive_resetRightHeight", UIActivityLoader.resetRightHeight);

        UIActivityLoader.onViewActivity(UIActivityLoader.portletId);
      }

    },
    resetRightHeight : function() {
      var wHeight = $(window).height();
      var leftBody = $('td.LeftNavigationTDContainer.TDContainer:first').height('auto');
      var rightTD = $('td.RightBodyTDContainer.TDContainer:first').attr('style', '');
      $('#UIUserActivityStreamPortlet').height('auto');
      var T = setTimeout(function(){
        rightTD.css('min-height', Math.max(wHeight, leftBody.height()) + 'px');
        clearTimeout(T);
      }, 1200);
    },
    setStatus : function(hasMore) {
      var me = UIActivityLoader;
      if(me.scrollBottom() <= me.delta) {
        $(window).scrollTop($(window).scrollTop()-5);
      }
      me.hasMore = (hasMore === true || hasMore === 'true');
      UIActivityLoader.initIndicator();
      me.processBottomTimeLine();
      
    },
    initIndicator : function() {
      $('#UIActivitiesLoader').find('div.ActivityIndicator').remove();
      var activityIndicator = $('<div class="ActivityIndicator" id="ActivityIndicator" style="display:none"></div>');
      for (var i=1; i < 9; i++) {
        activityIndicator.append($('<div id="rotateG_0' + i + '" class="blockG"></div>'));
      }
      activityIndicator.appendTo('#UIActivitiesLoader');
    },
    processBottomTimeLine : function() {
      //
      var me = UIActivityLoader;
      var loaderButton = $('#ActivitiesLoader');
      if ( me.hasMore ) {
        $('div.activityBottom').hide();
        loaderButton.parent().show();
      } else {
        $('div.activityBottom').show();
        loaderButton.parent().hide();
      }
    },
    renderActivity : function(activityItem) {
      var url = activityItem.data('url');
      if (url && url.indexOf('objectId') > 0) {
        url += activityItem.attr('id') + ((UIActivityLoader.getRequestParam().length > 0) ? UIActivityLoader.getRequestParam() : "");
        window.ajaxGet(url, function(data) {
          activityItem.attr('style', '').removeClass('activity-loadding');
          if (UIActivityLoader.responsiveId) {
            UIActivityLoader.onViewActivity(UIActivityLoader.responsiveId);
          }
        });
      }
    },
    getRequestParam : function() {
      var me = UIActivityLoader;
      if (me.requestParams === undefined || me.requestParams === null) {
        var h = window.location.href;
        if(h.indexOf('?') > 0 && h.indexOf('portal:componentId') < 0) {
          me.requestParams = '&' + h.substring(h.indexOf('?') + 1);
        } else {
          me.requestParams = "";
        }
      }
      return me.requestParams;
    },
    loadingActivities : function(id, numberOfReqsPerSec) {
      var me = UIActivityLoader;
      if (numberOfReqsPerSec === undefined) {
        numberOfReqsPerSec = me.numberOfReqsPerSec;
      }
      me.requestParams = null;
      var container = $('#' + id);
      var url = container.find('div.uiActivitiesLoaderURL:first').data('url');
      if (url === undefined || url.length === 0) {
        return;
      }
      var items = container.find('div.activity-loadding').data('url', url);
      var batchDelay = 1000;// 1000ms ~ 1s
      me.renderActivity(items.eq(0));
      var index = 1;
      var interval = window.setInterval(function() {
        if (index < items.length) {
          me.renderActivity(items.eq(index));
        } else {
          window.clearInterval(interval);
        }
        ++index;
      }, batchDelay / numberOfReqsPerSec);
    },
    addTop : function(activityItemId, responsiveId) {
      UIActivityLoader.responsiveId = responsiveId;
      var parentContainer = $('#UIActivitiesLoader');
      var activityContainer = parentContainer.find('div.uiActivitiesContainer:first');
      if($('#welcomeActivity').length === 0) {
        var url = activityContainer.find('div.uiActivitiesLoaderURL:first').data('url');
        if(activityContainer.find('#' + activityItemId).length <= 0 && url && url.length > 0) {
          var activityItem = $('<div class="uiActivityLoader activity-loadding" style="position:relative;" id="' + activityItemId + '"></div>');
          activityItem.data('url', url);
          //
          activityContainer.prepend(activityItem);
          UIActivityLoader.requestParams = null;
          if(UIActivityLoader.hasMore) {
            //Remove last
            parentContainer.find('.uiActivitiesContainer:last').find('.uiActivityLoader:last').remove();
          }
          //
          UIActivityLoader.renderActivity(activityItem);
        }
      }
    },
    onViewActivity: function(responsiveId) {
        var root = $('#'+responsiveId);
        if(root.length > 0 && eXo.social.SocialUtil.checkDevice().isMobile === true) {
        root.find('.activityStream').off('click').on('click', function(evt) {
            var parent = root;
            //only display the current activity
            var activity = $(this);                  
            if(activity.hasClass('block-activity')) {
              return true;
            }
            
            //hide all of activities
            parent.find('.activityStream').addClass('hidden-phone');
            //only show current
            activity.removeClass('hidden-phone').addClass('block-activity');
            //reset size of the stream
            $.publish("exo_social_activityStream_responsive_resetRightHeight");
            //
            $.publish("exo_social_activityStream_responsive_hide_top");

            var activityDisplay = parent.find('div.uiActivitiesDisplay:first').addClass('activityDisplay');   
            if(activityDisplay.find('.iconReturn').length === 0) {
              activityDisplay.prepend($('<div class="visible-phone" style="cursor:pointer"><i class="uiIconEcmsDarkGray uiIconEcmsReturn iconReturn"></i></div>').click(function() {
                var parent = root;
                //TODO raise event
                parent.find('div.uiActivitiesDisplay:first').removeClass('activityDisplay');
                parent.find('.activityStream').removeClass('hidden-phone');           
                var activity = parent.find('.block-activity').removeClass('block-activity');
                //
                $('.footComment').html('');
                $(this).remove();

                $.publish("exo_social_activityStream_responsive_show");
              }));
            }
            //
            
            //
            var footComment = $('.footComment');
            if(footComment.length === 0) {
              footComment = $('<div class="footComment visible-phone" style="z-index:1000"></div>');
              $('body').append(footComment);
            }
            var input = activity.find('.inputContainer:first').clone().removeClass('hidden-phone');
            window.inputId = input.attr('id');
            input.attr('id', 'CurrentCommentInput');
            input.find('.exo-mentions').remove();
            input.find('button.btn:first').attr('id', 'CurrentCommentButton');
            footComment.html('').append(input);
            footComment.find('textarea.textarea:first').attr('id', 'CurrentCommentTextare').exoMentions({
              onDataRequest:function (mode, query, callback) {
                var url = window.location.protocol + '//' + window.location.host + '/' + eXo.social.portal.rest + '/social/people/getprofile/data.json?search='+query;
                $.getJSON(url, function(responseData) {
                  responseData = _.filter(responseData, function(item) { 
                    return item.name.toLowerCase().indexOf(query.toLowerCase()) > -1;
                  });
                  callback.call(this, responseData);
                });
              },
              idAction : ('CurrentCommentButton'),
              elasticStyle : {
                maxHeight : '42px',
                minHeight : '32px',
                marginButton: '4px',
                enableMargin: false
              },
              messages : window.eXo.social.I18n.mentions
            });
            //
            var widthBtn = footComment.find('#CurrentCommentButton').on('click keyup', function(evt) {
              if(evt.type === 'keyup' && evt.keyCode !== 13) {
                return false;
              }
              var value = $(this).parents('.footComment').find('textarea.textarea:first').val();
              $('#' + window.inputId).find('textarea.textarea:first').val(value);
              var t = setTimeout(function() {
                clearTimeout(t);
                $.globalEval($('#' + window.inputId).find('button.btn:first').data('action-link'));
              }, 100);
            }).outerWidth();
            footComment.find('.commentInput:first').css('margin-right', widthBtn + 18 + 'px');
            //
            var commentList = activity.find('.commentListInfo:first');
            if(commentList.length > 0 && commentList.find('a:first').length > 0) {
              var action = commentList.find('a:first').attr('onclick');
              if(action && action.length > 0) {
                $.globalEval(action.replace('objectId=none', 'objectId=all'));
              }
            }
        }); 
      }
    }

  };
  return UIActivityLoader;
})($);