$('document').ready(() ->
	$('.test-btn').click(() -> window.open($('.test-btn').attr('href')))
)