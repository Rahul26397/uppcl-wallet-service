package com.tequre.wallet.enums;

public enum AgentRegistrationStates {

    SUBMITTED {
        @Override
        public Role role() {
            return Role.AGENT;
        }

    },

    PENDING {
        @Override
        public Role role() {
            return Role.AGENT;
        }

    },

    FIRST_LEVEL_APPROVAL {
        @Override
        public Role role() {
            return Role.UPPCL_APPROVAL_1;
        }

    },

    SECOND_LEVEL_APPROVAL {
        @Override
        public Role role() {
            return Role.UPPCL_APPROVAL_2;
        }

    },

    APPROVED {
        @Override
        public Role role() {
            return null;
        }

    },

    REJECTED {
        @Override
        public Role role() {
            return null;
        }

    };

    public abstract Role role();
}
