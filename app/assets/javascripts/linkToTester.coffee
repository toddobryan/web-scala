$('document').ready(() ->
	$('.test-btn').click(() -> window.open(this.attr('href')))
)