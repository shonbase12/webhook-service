# Updated retry conditions logic

def retry_condition(error):
    if error.is_transient():
        return True
    return False
