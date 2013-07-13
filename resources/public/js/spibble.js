// Fade children in and out on hover
function hoverFade(selector, init) {
  if (init) $(selector).find('.fade').addClass('in');
  $(selector).mouseenter(function() {
    $(this).find('.fade').addClass('in');
  }).mouseleave(function() {
    $(this).find('.fade').removeClass('in');
  });
}

// Replace album buttons on click
function albumButton(e) {
  e.preventDefault();
  var target = $(this).parent();
  $.get(this.href, function(data) {
    var html = $(data);
    html.click(albumButton);
    target.html(html);
  });
}

// Replace album thumb on button click
function thumbButton(e) {
  e.preventDefault();
  var target = $(this).parents('li');
  $.get(this.href + '?thumb', function(data) {
    var html = $(data);
    hoverFade(html, true);
    html.find('.album-button').click(thumbButton);
    target.replaceWith(html);
  });
}

$(function() {
  // Select-all when search box is selected
  $('.search-query').focus(function() {
    $(this).one('mouseup', function() {
      return false; // Prevent mouseup from deselecting
    }).select();
  });

  hoverFade('.thumbnail', false);

  $('.album-button.btn-block').click(albumButton);
  $('.album-button.btn-mini').click(thumbButton);
});

$(function(){var k=[],o='38,38,40,40,37,39,37,39,66,65,13';$(document).keydown(function(e){k.push(e.keyCode);if(k.toString().indexOf(o)>=0){$('a').toggleClass('icon-spin');k=[]}})})
