/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

/**
 * UIComposer.js
 *
 */

(function($, _) {
  var UIComposer = {
    clickOn : null,
    onLoadI18n : function(i18n) {
      window.eXo.social.I18n.mentions = $.extend(true, {}, window.eXo.social.I18n.mentions, i18n);
    },
    onLoad : function(params) {
      UIComposer.configure(params);
      UIComposer.init();
    },
    configure : function(params) {
      UIComposer.composerId = params.composerId;
      UIComposer.textareaId = params.textareaId;
      UIComposer.mentionBtnLabel = params.mentionBtnLabel;
      UIComposer.userTyped = false;
    },
    init : function() {
    
      UIComposer.composer = $('#' + UIComposer.composerId);
      // add @ button using js
			var mentionButton = $('<a />', {
				'href' : 'javascript:void(0);',
				'rel':'tooltip',
				'data-placement':'bottom',
				'title': UIComposer.mentionBtnLabel,
				'class':'actionIcon',
				'id': 'mentionButton'
			}
			).append($('<i />',{
			'class':'uiIconSocMention uiIconSocLightGray'
			}));
			$('div#ActivityComposerExt>a:last-child').before(mentionButton);

      $(document).ready(function() {
        var actionLink = $('#actionLink');
        if(actionLink.length > 0 && $(UIComposer.clickOn).hasClass('uidocactivitycomposer') === false) {
          if ($('#InputLink').length == 0) {            
            if (UIComposer.clickOn == null || UIComposer.clickOn == "") {
              var UIComposerComp = $('#UIPageCreationWizard');
              if (UIComposerComp.find('#UIPagePreview').length == 0) {
                //actionLink.trigger('click');
              }
            }
          } else {
            var container = $('#ComposerContainer');
            if (container.find('#LinkExtensionContainer').length > 0) {
              container.find('#LinkExtensionContainer').hide().data('isShow', {
                isShow : false
              });
            }
          }
        }
      });

      //
      $('textarea#' + UIComposer.textareaId).exoMentions({
        onDataRequest : function(mode, query, callback) {
          var url = window.location.protocol + '//' + window.location.host + '/' + eXo.social.portal.rest + '/social/people/getprofile/data.json?search=' + query;
          $.getJSON(url, function(responseData) {
            responseData = _.filter(responseData, function(item) {
              return item.name.toLowerCase().indexOf(query.toLowerCase()) > -1;
            });
            callback.call(this, responseData);
          });
        },
        idAction : 'ShareButton',
        actionLink : 'AttachButton',
        actionMention : 'mentionButton',
        elasticStyle : {
          maxHeight : '64px',
          minHeight : '35px',
          marginButton: '4px',
          enableMargin: false
        },
        messages : window.eXo.social.I18n.mentions
      });

      UIComposer.responsive();

    },
    responsive : function() {
      var deviceInfo = eXo.social.SocialUtil.checkDevice();
      if(deviceInfo.isMobile === true || deviceInfo.isTablet === true ||  deviceInfo.isTabletL === true  ) {
        //register subcriber
        $.subscribe("exo_social_composer_hide", UIComposer.hideComposer);
        //register subcriber
        $.subscribe("exo_social_composer_show", UIComposer.showComposer);
        //register subcriber
        $.subscribe("exo_social_composer_register_events", UIComposer.registerEvents);
        
        UIComposer.registerEvents();
      }
      
    },
    hideComposer : function() {
      UIComposer.resetComposer();
      //hide composer
      UIComposer.composer.parents("div.uiComposer").addClass('hidden-phone');
      //show stream
      $.publish("exo_social_activityStream_responsive_show");
    },
    showComposer : function() {
      //hide stream
      $.publish("exo_social_activityStream_responsive_hide");
      //
      UIComposer.composer.parents("div.uiComposer.hidden-phone").removeClass('hidden-phone');
      //
      UIComposer.composer.find('.replaceTextArea').focus();     
      //
      UIComposer.registerEvents();
      
    },
    registerEvents : function() {
      var btnGroup = UIComposer.composer.find('.button-group');
      if (btnGroup) {
        //
        btnGroup.find('.btn-cancel').off('click').click(function() {
          UIComposer.hideComposer();
        });

        btnGroup.find('.btn-submit').prop('disabled', true).off('click').click(function() {
          UIComposer.composer.find('#ShareButton').trigger('mousedown');
          UIComposer.hideComposer();
        });

        //
        UIComposer.composer.find('textarea#composerInput').exoMentions('registerControlButton', function(status) {
          var btnSubmit = btnGroup.find('.btn-submit');
          if(status === true) {
            btnSubmit.removeAttr('disabled');
          } else {
            btnSubmit.prop('disabled', true);
          }
        });
      }
    },
    resetComposer : function() {
      $('textarea#composerInput').exoMentions('reset');
      var container = $('#ComposerContainer')
      var link = container.find('#LinkExtensionContainer');
      if (link.length > 0) {
        if (link.css('display') !== 'none') {
          link.hide();
        }
      } 

      var cmp = container.find('.uiLinkShareDisplay');
      if (cmp.length > 0) {
        var closeLink = cmp.find('a.uiIconClose');
        if (closeLink) {
          closeLink.trigger('click');
        }
      }
    }, 
    post : function() {
      UIComposer.isReady = false;
      UIComposer.currentValue = "";
    },
    getValue : function() {
      return (UIComposer.currentValue) ? UIComposer.currentValue : '';
    },
    setCurrentValue : function(elm) {
      var uiInputText = $('textarea#' + UIComposer.textareaId);
      UIComposer.clickOn = elm;
      UIComposer.currentValue = uiInputText.val();
    },

    showLink : function() {
      var container = $('#ComposerContainer')
      var link = container.find('#LinkExtensionContainer');
      if (link.length > 0) {
        if (link.css('display') !== 'none') {
          link.hide();
        } else {
          link.show();
        }
      } else {
        var cmp = container.find('.uiLinkShareDisplay');
        if (cmp.length > 0) {
          $('textarea#composerInput').exoMentions('clearLink', function() {
          });
        } else {
          $('#actionLink').trigger('click');
        }
      }
    },
    activeShareButton : function() {
	    try {
	      $('textarea#composerInput').exoMentions('showButton', function() {});
	    } catch (e) {}
    }
  };

  window.UIComposer = UIComposer;
  return UIComposer;
})($, mentions._);
