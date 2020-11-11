/*
 * lecture.js is a Javascript library to display video lectures in a webpage.
 *
 * Copyright 2012 Alastair R. Beresford
 *
 * lecture.js is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lecture.js is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero Public License for more details.
 *
 * This library requires an HTML page, video.js, quiz.js and a JSON data file.
 */

(function($){
	$(document).ready(function() {
		$('#quiz').on('mousemove', function(e) {
			var x = e.pageX - this.offsetLeft;
			var y = e.pageY - this.offsetTop;
			$('#videoMouseLocation').html('(' + x +', '+ y + ')');
		});
	});
})(jQuery);

//Initialise the lecture. Requires data in JSON format to render.
var lecture = function(videoname, videodata) {

	_V_.options.flash.swf = 'video-js.swf';

	_V_('video').ready(function() {
		var myPlayer = this; 

		$('#pauseVideoTickbox').on('click', function(e) {
			if ($('#quiz > p').length > 0 &&
					!$('#pauseVideoTickbox').is(':checked')) { //quiz has paused video
				myPlayer.play();
			}
		});

		//Given an ordered array of integers, find the largest index
		//into array which is less than 'value' (return first element if no match)
		var currentSlide = function(orderedArray, value) {
			for(var i = 0; i < orderedArray.length; i++) {
				if (orderedArray[i] > value) {
					return (i == 0) ? 0 : i-1;
				}
			}
			return orderedArray.length - 1;
		};

		//Seek video forward or backward by a specified amount of time
		var slideSeek = function(time) {
			return function(e) {
				eventUpdate(eventUrl, eventMap.SLIDE_SEEK_RELATIVE, videoname + ',' + myPlayer.currentTime() + ',' + time);
				myPlayer.currentTime(myPlayer.currentTime() + time);
				myPlayer.play();
				if (e.preventDefault) {
					e.preventDefault();
				} else {
					e.returnValue = false; //<=IE8
				}
			};
		};

		//Given a type of slide index, and an index value, play from that index; the log value may be true or false
		var slideSkip = function(type, index, log) { 
			return function(e) {
				var msg = videoname + ',' + myPlayer.currentTime() + ',' + index + ',' + videodata.indexes[type][index];
				if (log) {
					if (type == 'question') {
						eventUpdate(eventUrl, eventMap.QUESTION_CHANGE_ABSOLUTE, msg);
					} else {
						eventUpdate(eventUrl, eventMap.SLIDE_CHANGE_ABSOLUTE, msg);					
					}
				}
				myPlayer.currentTime(videodata.indexes[type][index]);
				myPlayer.play();
				if (e != null && e.preventDefault) {
					e.preventDefault();
				} else if (e != null) {
					e.returnValue = false; //<=IE8
				}
			};
		};

		//Given a type of slide index and an increment or decrement step value
		//play video from that new point
		var slideStep = function(type, step) {
			return function(e) {
				var originalStep = step;
				var p = currentSlide(videodata.indexes[type], myPlayer.currentTime());
				if (p + step >= videodata.indexes[type].length) {
					step = videodata.indexes[type].length - p + 1;
				}
				if (p + step < 0) {
					step = -p;
				}
				var msg = videoname + ',' + myPlayer.currentTime() + ',' + p + ',' + originalStep + "," + step;
				if (type == 'question') {
					eventUpdate(eventUrl, eventMap.QUESTION_CHANGE_RELATIVE, msg);
				} else {
					eventUpdate(eventUrl, eventMap.SLIDE_CHANGE_RELATIVE, msg);					
				}
				slideSkip(type, p + step, false)(e);
			};
		};

		//attach video controls to the appropriate HTML elements
		$('#slideNext').on('click', slideStep('slide', 1));
		$('#slideRepeat').on('click', slideStep('slide', 0));
		$('#slidePrev').on('click', slideStep('slide', -1));
		$('#questionNext').on('click', slideStep('question', 1));
		$('#questionRepeat').on('click', slideStep('question', 0));
		$('#questionPrev').on('click', slideStep('question', -1));
		for(var type in videodata.indexes) {
			for(var i = 0; i < videodata.indexes[type].length; i++) {
				var a = $('<a href="">' + (i + 1) + '</a>');
				a.addClass('slideControl');
				a.on('click', slideSkip(type, i, true));
				a.attr('id', type + 'Index' + i);
				$('#' + type + 'Index').append(a).append('&nbsp;');
			}
		}
		$('#slidePlus5').on('click', slideSeek(5));
		$('#slideMinus5').on('click', slideSeek(-5));

		//Mark the first slide as the currently playing one
		$('#slideIndex0').addClass('currentSlide');

		//Inject speed controls
		var adjustSpeed = function(playbackRate) {
			return function(e) {
				var usingHtml5 = $('#video').find('video').length > 0;
				if (usingHtml5) {
					var html5Video = $('#video').find('video')[0];
					html5Video.playbackRate = playbackRate;
				}
			};
		};
		var supportedSpeeds = [0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3];
		for(var i = 0; i < supportedSpeeds.length; i++) {
			var a = $('<a href="">' + supportedSpeeds[i] + '</a>');
			a.addClass('slideControl');
			a.on('click', adjustSpeed(supportedSpeeds[i]));
			a.attr('id', 'speedIndex' + i);
			$('#speedIndex').append(a).append('&nbsp;');
		}
		
		//Detect whether we are using the flash plugin or not; hide overlay option if so
		var usingFlash = $('#video').find('object').length > 0;
		if (usingFlash) {
			$('#absolutePosition').attr('checked', false);
			$('#absolutePositionText').css('display', 'none');
			$('#speedIndex').css('display', 'none');
		}
		
		//Keep track of the last time we received a callback from the video
		//Use this info to infer whether we need to pause the video for a question
		var lastTime = 0;
		var currentTime = 0;
		
		//Keep track of the last time we told the server we were watching the video
		var tickReportDiff = 20;
		var timeSinceLastTick = 0;
		
		myPlayer.addEvent('timeupdate',function() {

			lastTime = currentTime;
			currentTime = myPlayer.currentTime();
			
			if (timeSinceLastTick > tickReportDiff) {
				eventUpdate(eventUrl, eventMap.VIDEO_TIMER_TICK, videoname + ',' + currentTime + ',' + timeSinceLastTick);
				timeSinceLastTick = 0;
			} else {
				if (currentTime > lastTime) {
					timeSinceLastTick += currentTime - lastTime;
				} else {
					timeSinceLastTick += lastTime - currentTime;					
				}
			}

			$('#timeInSecs').html(timeSinceLastTick.toFixed(1) + ',' + currentTime.toFixed(1) + ',' + lastTime.toFixed(1));
			//$('#timeInSecs').html(currentTime.toFixed(2));

			var slideIndex = currentSlide(videodata.indexes['slide'], currentTime);
			var slideId = '#slideIndex' + slideIndex;
			if (!$(slideId).hasClass('currentSlide')) {
				$('#slideIndex').find('a').removeClass('currentSlide');
				$(slideId).addClass('currentSlide');
			}

			if ($('#pauseVideoTickbox').is(':checked')) {
				$.each(videodata.questions, function(qName, qDetail) {
					qTime = qDetail['time'];
					if (lastTime < qTime && currentTime > qTime &&
							currentTime - lastTime < 2) {
						var callback = function() {
							myPlayer.play();
						};
						if (qDetail.answertype == 'skip') {
							callback = slideStep('slide', 1);
						}
						var options = {'question': qDetail,
								'questionName': qName,
								'videoName': videoname,
								'questionDiv': '#quiz',
								'callWhenCorrect': callback,
								'absolutePosition': $('#absolutePosition').is(':checked')};
						$('#video').quiz(options);
						if (qDetail.answertype != 'skip') {
							myPlayer.pause();
						}
					}
				});
			}
		});

		myPlayer.addEvent('play', function() {
			$('#video').quiz('remove');
			eventUpdate(eventUrl, eventMap.VIDEO_PLAY, videoname + ',' + myPlayer.currentTime() + ', play');
		}); 

		myPlayer.addEvent('pause', function() {
			eventUpdate(eventUrl, eventMap.VIDEO_PAUSE, videoname + ',' + myPlayer.currentTime() + ', pause');
		}); 
	});     
};