def test_retry_behavior():
    assert retry_function() == expected_result

def test_idempotency_key_behavior():
    assert idempotent_function(key='test_key') == expected_result
