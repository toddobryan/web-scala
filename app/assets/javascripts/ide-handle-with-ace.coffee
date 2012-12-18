ajax = ->
	$('#code').submit((e) ->
        alert(editor.getValue())
        $('#line').value(editor.getValue())
        alert($('#code').serialize())
        $.post('/interpret', $('#code').serialize(), (result) ->
            $('.prev-content').append('<p>&gt; ' + editor.getValue() + '</p>')
            $('.prev-content').append('<p>' + result + '</p>')
            editor.setValue("")
        )
        e.preventDefault()
    )

$('document').ready(->
    ajax()
)