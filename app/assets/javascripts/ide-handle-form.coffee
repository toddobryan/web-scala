$('document').ready(->
    $('#code').submit((e) ->
        $.post('/interpret', $('#code').serialize(), (result) ->
            $('.prev-content').append('<p>&gt; ' + $('#line').val() + '</p>')
            $('.prev-content').append('<p>' + result + '</p>')
            $('#line').val('')
        )
        e.preventDefault()
    )
)