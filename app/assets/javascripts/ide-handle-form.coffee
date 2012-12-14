$('document').ready(->
    $('#code').submit((e) ->
        typed = $('input[name=what]').val()
        $('.prev-content').append('<p>&gt; ' + typed + '</p>')
        $('input[name=what]').val('')
        return false
    )
)