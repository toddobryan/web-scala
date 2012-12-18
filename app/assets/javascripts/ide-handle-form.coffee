$('document').ready(->
    $('#code').submit((e) ->
        $.post('/interpret', $('#editor').serialize(), (result) ->
            $('.prev-content').append('<p>&gt; ' + editor.getValue() + '</p>')
            $('.prev-content').append('<p>' + result + '</p>')
            editor.setValue("")
        )
        e.preventDefault()
    )
)